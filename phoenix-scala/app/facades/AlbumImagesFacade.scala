package facades

import java.nio.file.Files
import java.time.ZonedDateTime

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

    case class S3Path(dir: String, fileName: String) {
      def absPath(): String = {
        s"$dir/$fileName"
      }
    }

    object S3Path {
      def get(album: Option[Album], fileName: String): S3Path = {
        val now        = ZonedDateTime.now()
        val year: Int  = now.getYear
        val month: Int = now.getMonthValue

        val prefix = s"${now.getDayOfMonth}${now.getHour}${now.getMinute}${now.getSecond}"

        album.fold {
          S3Path(dir = s"other/$year/$month", fileName = s"$prefix$fileName")
        } { album ⇒
          S3Path(dir = s"albums/${album.contextId}/${album.formId}", fileName = fileName)
        }
      }
    }

    def uploadImagesR(imageSource: T, context: OC, album: Option[Album])(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        sys: ActorSystem,
        apis: Apis): DbResultT[Seq[ImagePayload]]

    def uploadImages(imageSource: T, contextName: String)(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        sys: ActorSystem,
        apis: Apis): DbResultT[Seq[ImagePayload]] =
      for {
        context ← * <~ ObjectManager.mustFindByName404(contextName)
        result  ← * <~ uploadImagesR(imageSource, context, album = None)
      } yield result

    def uploadImagesToAlbum(
        album: Album,
        context: OC,
        imageSource: T)(implicit ec: EC, db: DB, au: AU, am: Mat, sys: ActorSystem, apis: Apis) =
      for {
        _     ← * <~ uploadImagesR(imageSource, context, Some(album))
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

  def uploadImagesFromMultiPart(contextName: String, formData: Multipart.FormData)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      sys: ActorSystem,
      apis: Apis): DbResultT[Seq[ImagePayload]] = {

    MultipartUploader.uploadImages(formData, contextName)
  }

  // - endpoints

  def attachImageToAlbum(album: Album, payload: ImagePayload)(
      implicit ec: EC,
      db: DB,
      au: AU,
      oc: OC): DbResultT[Seq[FullObject[Image]]] =
    for {
      existingImages ← * <~ AlbumImageLinks.queryRightByLeft(album)
      newPayloads = existingImages.map(imageToPayload) :+ payload
      objectImages ← * <~ createOrUpdateImagesForAlbum(album, newPayloads, oc)
    } yield objectImages

  def attachImageTo(payload: ImagePayload, context: OC, maybeAlbum: Option[Album])(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[Seq[FullObject[Image]]] = {
    implicit val oc = context
    maybeAlbum.fold {
      createOrUpdateImages(Seq(payload), oc)
    } { album ⇒
      attachImageToAlbum(album, payload)
    }
  }

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
                               s3Path: S3Path)(implicit ec: EC, am: Mat, apis: Apis) = {
      for {
        url ← apis.amazon.uploadFileF(s3Path.absPath, filePath.toFile)
        _ = Files.deleteIfExists(filePath)
      } yield ImageUploaded(url = url, fileName = s3Path.fileName)
    }

    def uploadImagesR(formData: Multipart.FormData, context: OC, maybeAlbum: Option[Album])(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        sys: ActorSystem,
        apis: Apis): DbResultT[Seq[ImagePayload]] = {
      implicit val oc = context

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
            val s3path = S3Path.get(maybeAlbum, fileName = fileName)
            uploadPathToS3(filePath, s3path)
        }
        .map { srcInfo ⇒
          val payload = ImagePayload(src = srcInfo.url,
                                     title = srcInfo.fileName.some,
                                     alt = srcInfo.fileName.some)

          attachImageTo(payload, context, maybeAlbum).map(_.map(imageToPayload))

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
    def uploadImagesR(payload: ImagePayload, context: OC, maybeAlbum: Option[Album])(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        sys: ActorSystem,
        apis: Apis): DbResultT[Seq[ImagePayload]] = {

      for {
        imageData ← * <~ fetchImageData(payload.src)
        url ← * <~ saveBufferAndThen[String](imageData) { path ⇒
               val fileName = extractFileNameFromUrl(payload.src)
               val s3Path   = S3Path.get(maybeAlbum, fileName = fileName)
               DbResultT.fromResult(apis.amazon.uploadFile(s3Path.absPath, path.toFile))
             }
        newPayload = payload.copy(src = url)

        payloads ← * <~ attachImageTo(newPayload, context, maybeAlbum)

      } yield payloads.map(imageToPayload)
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
