package facades

import java.nio.file.Files

import scala.concurrent.Future
import akka.actor.ActorSystem
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

    def uploadImagesR(album: Option[(Album, OC)], imageSource: T)(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        sys: ActorSystem,
        apis: Apis): DbResultT[Seq[ImagePayload]]

    def s3DirectoryPath(album: Option[Album]): String = {
      album.fold(s"/other/") { album ⇒
        s"albums/${album.contextId}/${album.formId}"
      }
    }

    def uploadImages(
        imageSource: T)(implicit ec: EC, db: DB, au: AU, am: Mat, sys: ActorSystem, apis: Apis) =
      uploadImagesR(None, imageSource)

    def uploadImagesToAlbum(
        album: Album,
        context: OC,
        imageSource: T)(implicit ec: EC, db: DB, au: AU, am: Mat, sys: ActorSystem, apis: Apis) =
      for {
        _     ← * <~ uploadImagesR(Some(album, context), imageSource)
        album ← * <~ getAlbumInner(album.formId, context)
      } yield album
  }

  private def uploadImagesToAlbum[T](albumId: Int, contextName: String, imageSource: T)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      sys: ActorSystem,
      imageUploader: ImageUploader[T],
      apis: Apis): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ mustFindAlbumByFormIdAndContext404(albumId, context)
      _       ← * <~ DbResultT.fromEither(album.mustNotBeArchived)

      result ← * <~ imageUploader.uploadImagesToAlbum(album, context, imageSource)
    } yield result

  //+ endpoints
  def uploadImagesFromPayloadToAlbum(albumId: Int, contextName: String, payload: ImagePayload)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      sys: ActorSystem,
      apis: Apis): DbResultT[AlbumRoot] = {
    implicit val imageUploader = ImagePayloadUploader
    uploadImagesToAlbum[ImagePayload](albumId, contextName, payload)
  }

  def uploadImagesFromMultipartToAlbum(albumId: Int,
                                       contextName: String,
                                       formData: Multipart.FormData)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      sys: ActorSystem,
      apis: Apis): DbResultT[AlbumRoot] = {
    implicit val imageUploader = MultipartUploader
    uploadImagesToAlbum[Multipart.FormData](albumId, contextName, formData)
  }

  def uploadImagesFromMultiPart(formData: Multipart.FormData)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      sys: ActorSystem,
      apis: Apis): DbResultT[Seq[ImagePayload]] = {

    MultipartUploader.uploadImages(formData)
  }

  // - endpoints

  def attachImageToAlbum(album: Album, payload: ImagePayload)(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU,
      mat: Mat,
      apis: Apis): DbResultT[Seq[FullObject[Image]]] =
    for {
      existingImages ← * <~ AlbumImageLinks.queryRightByLeft(album)
      newPayloads = existingImages.map(imageToPayload) :+ payload
      objectImages ← * <~ createOrUpdateImagesForAlbum(album, newPayloads, oc)
    } yield objectImages

  case class ImageUploaded(url: String, fileName: String)

  object MultipartUploader extends ImageUploader[Multipart.FormData] {

    private def getFileFromRequest(bytes: Source[ByteString, Any])(implicit ec: EC, am: Mat) = {
      val file = Files.createTempFile("tmp", ".jpg")
      bytes.runWith(FileIO.toPath(file)).map { ioResult ⇒
        if (ioResult.wasSuccessful) Either.right(file)
        else Either.left(ErrorReceivingImage.single)
      }
    }

    private def uploadPathToS3(filePath: java.nio.file.Path,
                               fileName: String,
                               directoryPath: String)(implicit ec: EC, am: Mat, apis: Apis) = {
      for {
        url ← apis.amazon.uploadFileF(s"$directoryPath/$fileName", filePath.toFile)
        _ = Files.deleteIfExists(filePath)
      } yield ImageUploaded(url = url, fileName = fileName)
    }

    def uploadImagesR(maybeAlbum: Option[(Album, OC)], formData: Multipart.FormData)(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        sys: ActorSystem,
        apis: Apis): DbResultT[Seq[ImagePayload]] = {

      val uploadedImages = formData.parts
        .filter(_.name == "upload-file")
        .mapAsyncUnordered(4) { part ⇒
          for {
            filePathE ← getFileFromRequest(part.entity.dataBytes)
            filePath = filePathE.leftMap { _ ⇒
              ImageFacadeException(ErrorReceivingImage)
            }.toTry.get
            fileName = part.filename.getOrElse(
                throw ImageFacadeException(ImageFilenameNotFoundInPayload))
          } yield (filePath, fileName)
        }
        .mapAsyncUnordered(4) {
          case (filePath, fileName) ⇒
            val directoryPath = s3DirectoryPath(maybeAlbum.map(_._1))
            uploadPathToS3(filePath, fileName, directoryPath)
        }
        .map { srcInfo ⇒
          val payload = ImagePayload(src = srcInfo.url,
                                     title = srcInfo.fileName.some,
                                     alt = srcInfo.fileName.some)

          maybeAlbum.fold(DbResultT.good(Seq(payload))) {
            case (album, context) ⇒
              implicit val oc = context
              attachImageToAlbum(album, payload).map(_.map(imageToPayload))
          }

        }
        .runReduce[DbResultT[Seq[ImagePayload]]] {
          case (a, b) ⇒
            DbResultT.seqCollectFailures(List(a, b)).map { e ⇒
              e.flatten
            }
        }
        .recover {
          case _: NoSuchElementException ⇒
            DbResultT.failure(ImageNotFoundInPayload)
          case e: ImageFacadeException ⇒
            DbResultT.failure(e.underlyingFailure)
          case e: Throwable ⇒
            logger.error(s"Error during upload files: $e")
            DbResultT.failure(ImageUploadFailedGeneralFailure(e))
        }

      for {
        images   ← * <~ DBIO.from(uploadedImages)
        payloads ← * <~ images
      } yield payloads
    }

  }

  object ImagePayloadUploader extends ImageUploader[ImagePayload] {
    def uploadImagesR(maybeAlbum: Option[(Album, OC)], payload: ImagePayload)(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        sys: ActorSystem,
        apis: Apis): DbResultT[Seq[ImagePayload]] = {

      for {
        imageData ← * <~ fetchImageData(payload.src)
        s3Path = s3DirectoryPath(maybeAlbum.map(_._1))
        url ← * <~ saveBufferAndThen[String](imageData) { path ⇒
               val fileName = extractFileNameFromUrl(payload.src)
               val fullPath = s"$s3Path/$fileName"
               DbResultT.fromResult(apis.amazon.uploadFile(fullPath, path.toFile))
             }

        newPayload = payload.copy(src = url)

        payloads ← * <~ maybeAlbum.fold(DbResultT.good(Seq(newPayload))) {
                    case (album, context) ⇒
                      implicit val oc = context
                      attachImageToAlbum(album, newPayload).map(_.map(imageToPayload))
                  }

      } yield payloads
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
