package facades

import java.nio.file.Files

import scala.concurrent.Future
import akka.http.scaladsl.model.{HttpRequest, Multipart}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString

import cats.implicits._
import de.heikoseeberger.akkasse.EventStreamElement
import failures.Failures
import failures.ImageFailures._
import models.image._
import models.objects.FullObject
import slick.dbio.DBIO
import payloads.ImagePayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import services.image.ImageManager._
import services.objects.ObjectManager
import utils.aliases._
import utils.apis.Apis
import utils.db._
import utils.{IlluminateAlgorithm, JsonFormatters}

object AlbumImagesFacade extends ImageFacade {

  implicit def formats = JsonFormatters.phoenixFormats

  case class ImageFacadeException(underlyingFailure: failures.Failure) extends Throwable

  trait ImageUploader[T] {
    def uploadImages(
        album: Album,
        imageSource: T,
        context: OC)(implicit ec: EC, db: DB, au: AU, am: Mat, apis: Apis): DbResultT[AlbumRoot]
  }

  private def uploadImages[T](albumId: Int, contextName: String, imageSource: T)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      imageUploader: ImageUploader[T],
      apis: Apis): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ mustFindAlbumByFormIdAndContext404(albumId, context)
      _       ← * <~ album.mustNotBeArchived
      result  ← * <~ imageUploader.uploadImages(album, imageSource, context)
    } yield result

  def uploadImagesFromPayload(albumId: Int, contextName: String, payload: ImagePayload)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      apis: Apis): DbResultT[AlbumRoot] = {
    implicit val imageUploader = ImagePayloadUploader
    uploadImages[ImagePayload](albumId, contextName, payload)
  }

  def uploadImagesFromMultipart(albumId: Int, contextName: String, formData: Multipart.FormData)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      apis: Apis): DbResultT[AlbumRoot] = {
    implicit val imageUploader = MultipartUploader
    uploadImages[Multipart.FormData](albumId, contextName, formData)
  }

  def attachImageToAlbum(album: Album, srcInfo: ImageUploaded)(implicit ec: EC,
                                                               db: DB,
                                                               oc: OC,
                                                               au: AU,
                                                               mat: Mat,
                                                               apis: Apis): DbResultT[Unit] =
    for {
      existingImages ← * <~ AlbumImageLinks.queryRightByLeft(album)
      payload = existingImages.map(imageToPayload) :+
        ImagePayload(src = srcInfo.url, title = srcInfo.fileName.some, alt = srcInfo.fileName.some)

      _ ← * <~ createOrUpdateImagesForAlbum(album, payload, oc)
    } yield {}

  case class ImageUploaded(url: String, fileName: String)

  object MultipartUploader extends ImageUploader[Multipart.FormData] {

    private def uploadBodyToS3(part: Multipart.FormData.BodyPart,
                               directoryPath: String)(implicit ec: EC, am: Mat, apis: Apis) = {
      for {
        filePathE ← getFileFromRequest(part.entity.dataBytes)
        filePath = filePathE.leftMap { _ ⇒
          ImageFacadeException(ErrorReceivingImage)
        }.toTry.get
        fileName = part.filename.getOrElse(
            throw ImageFacadeException(ImageFilenameNotFoundInPayload))

        fullPath = s"$directoryPath/$fileName"

        url ← apis.amazon.uploadFileF(fullPath, filePath.toFile)
      } yield ImageUploaded(url = url, fileName = fileName)
    }

    def uploadImages(album: Album, formData: Multipart.FormData, context: OC)(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        apis: Apis): DbResultT[AlbumRoot] = {
      val failures                              = ImageNotFoundInPayload.single
      val error: Future[Either[Failures, Unit]] = Future.successful(Either.left(failures))

      implicit val oc = context

      val attachToAlbum = attachImageToAlbum(album, _: ImageUploaded)

      val uploadedImages = formData.parts
        .filter(_.name == "upload-file")
        .mapAsync(1) { part ⇒
          val directoryPath = s"albums/${oc.id}/${album.formId}"
          uploadBodyToS3(part, directoryPath)
        }
        .map(attachToAlbum)
        .runReduce[DbResultT[Unit]] {
          case (a, b)                                             ⇒
            DbResultT.seqCollectFailures(List(a, b)).map { case _ ⇒ () }
        }

      for {
        images ← * <~ DBIO.from(uploadedImages)
        _      ← * <~ images
        album  ← * <~ getAlbumInner(album.formId, oc)
      } yield album
    }

  }

  object ImagePayloadUploader extends ImageUploader[ImagePayload] {
    def uploadImages(album: Album, payload: ImagePayload, context: OC)(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        apis: Apis): DbResultT[AlbumRoot] = {
      implicit val oc = context

      for {
        imageData ← * <~ fetchImageData(payload.src)
        url ← * <~ saveBufferAndThen[String](imageData) { path ⇒
               val fileName = extractFileNameFromUrl(payload.src)
               val fullPath = s"albums/${oc.id}/${album.formId}/$fileName"
               DbResultT.fromResult(apis.amazon.uploadFile(fullPath, path.toFile))
             }

        existingImages ← * <~ AlbumImageLinks.queryRightByLeft(album)
        newPayload = existingImages.map(imageToPayload) :+
          payload.copy(src = url)

        _     ← * <~ createOrUpdateImagesForAlbum(album, newPayload, oc)
        album ← * <~ getAlbumInner(album.formId, oc)

      } yield album
    }
  }

  private def getFileFromRequest(bytes: Source[ByteString, Any])(implicit ec: EC, am: Mat) = {
    val file = Files.createTempFile("tmp", ".jpg")
    bytes.runWith(FileIO.toPath(file)).map { ioResult ⇒
      if (ioResult.wasSuccessful) Either.right(file)
      else Either.left(ErrorReceivingImage.single)
    }
  }

  def imageToPayload(image: FullObject[Image]): ImagePayload = {
    val form   = image.form.attributes
    val shadow = image.shadow.attributes

    val src     = IlluminateAlgorithm.get("src", form, shadow).extract[String]
    val title   = IlluminateAlgorithm.get("title", form, shadow).extractOpt[String]
    val alt     = IlluminateAlgorithm.get("alt", form, shadow).extractOpt[String]
    val baseUrl = IlluminateAlgorithm.get("baseUrl", form, shadow).extractOpt[String]

    ImagePayload(Some(image.model.id), src = src, title = title, alt = alt, baseUrl = baseUrl)
  }

}
