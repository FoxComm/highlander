package phoenix.facades

import java.io.{BufferedInputStream, ByteArrayInputStream, FileInputStream, InputStream}
import java.net.{URL, URLConnection}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Files, Path}
import java.time.ZonedDateTime

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.Try
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import core.db._
import core.failures.{Failure, Failures}
import core.utils.generateUuid
import objectframework.IlluminateAlgorithm
import objectframework.models.FullObject
import objectframework.services.ObjectManager
import phoenix.failures.ImageFailures._
import phoenix.models.image._
import phoenix.payloads.ImagePayloads._
import phoenix.responses.AlbumResponses.AlbumResponse
import phoenix.services.image.ImageManager._
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import slick.dbio.DBIO

object ImageFacade extends ImageHelpers {
  implicit val formats = JsonFormatters.phoenixFormats

  val allowedUrlSchemes: Set[String] = Set("http", "https")

  case class ImageFacadeException(underlyingFailure: core.failures.Failure) extends Throwable

  trait ImageUploader[T] {

    case class S3Path(dir: String, fileName: String) {
      def absPath: String =
        s"$dir/$fileName"
    }

    object S3Path {

      /** change extension to standard one
        * if provided extension from fileName doesn't match with mediaType
        * */
      private def fixExtension(fileName: String, mediaType: MediaType): String = {
        val extFromMedia = mediaType.fileExtensions.last
        fileName.lastIndexOf('.') match {
          case -1 ⇒ s"$fileName.$extFromMedia"
          case idx ⇒
            val ext = fileName.substring(idx)
            if (MediaTypes.forExtension(ext) == mediaType)
              fileName
            else
              s"${fileName.slice(0, idx)}.$extFromMedia"
        }
      }

      def get(album: Option[Album], fileName: String, mediaType: MediaType): S3Path = {
        val now        = ZonedDateTime.now()
        val year: Int  = now.getYear
        val month: Int = now.getMonthValue

        val prefix    = s"${now.getDayOfMonth}${now.getHour}${now.getMinute}${now.getSecond}"
        val finalName = fixExtension(fileName, mediaType)

        album.fold {
          S3Path(dir = s"other/$year/$month", fileName = s"$prefix$finalName")
        } { album ⇒
          S3Path(dir = s"albums/${album.contextId}/${album.formId}", fileName = finalName)
        }
      }
    }

    def uploadImages(imageSource: T, contextName: String)(implicit ec: EC,
                                                          db: DB,
                                                          au: AU,
                                                          am: Mat,
                                                          sys: ActorSystem,
                                                          apis: Apis): DbResultT[Seq[ImagePayload]] =
      for {
        context ← * <~ ObjectManager.mustFindByName404(contextName)
        result  ← * <~ uploadImagesR(imageSource, context, album = None)
      } yield result

    def uploadImagesR(imageSource: T, context: OC, album: Option[Album])(
        implicit ec: EC,
        db: DB,
        au: AU,
        am: Mat,
        sys: ActorSystem,
        apis: Apis): DbResultT[Seq[ImagePayload]]

    def uploadImagesToAlbum(
        album: Album,
        context: OC,
        imageSource: T)(implicit ec: EC, db: DB, au: AU, am: Mat, sys: ActorSystem, apis: Apis) =
      for {
        _     ← * <~ uploadImagesR(imageSource, context, Some(album))
        album ← * <~ getAlbumInner(album.formId, context)
      } yield album

  }

  def validateImageUrl(url: String): Either[Failures, Uri] =
    Try {
      val normalized = new URL(url).toURI.toASCIIString
      val uri        = Uri(normalized)
      if (uri.isRelative || uri.isEmpty || !allowedUrlSchemes.contains(uri.scheme))
        Either.left(InvalidImageUrl(url).single)
      else
        Either.right(uri)
    }.getOrElse(Either.left(InvalidImageUrl(url).single))

  object ImageUploader {

    sealed trait ContentTypeGuess[T] {
      protected def unsafeGuessContentType(src: T): String

      def guessContentType(src: T): Option[String] =
        Try {
          Option(unsafeGuessContentType(src))
        }.toOption.flatten
    }

    private object ContentTypeGuess {

      implicit object BBContentTypeGuess extends ContentTypeGuess[ByteBuffer] {

        private def asInputStream(buffer: ByteBuffer): InputStream = {
          if (buffer.hasArray) { // use heap buffer; no array is created; only the reference is used
            new ByteArrayInputStream(buffer.array)
          }
          new utils.io.ByteBufferInputStream(buffer)
        }

        protected def unsafeGuessContentType(bb: ByteBuffer): String =
          URLConnection.guessContentTypeFromStream(asInputStream(bb))
      }

      implicit object ISContentTypeGuess extends ContentTypeGuess[InputStream] {
        protected def unsafeGuessContentType(is: InputStream): String =
          URLConnection.guessContentTypeFromStream(is)
      }

      implicit object PathContentTypeGuess extends ContentTypeGuess[Path] {
        protected def unsafeGuessContentType(path: Path): String = {
          val is = new BufferedInputStream(new FileInputStream(path.toAbsolutePath.toString))
          try URLConnection.guessContentTypeFromStream(is)
          finally is.close()
        }
      }
    }

    private val allowedImageTypes: List[MediaType] =
      List(MediaTypes.`image/gif`, MediaTypes.`image/png`, MediaTypes.`image/jpeg`)

    private val allowedImageTypeValues: List[String] =
      allowedImageTypes.map(_.value)

    def shouldBeValidImage[T](imageData: T)(implicit ct: ContentTypeGuess[T]): Either[Failures, MediaType] =
      ct.guessContentType(imageData)
        .map { contentType ⇒
          allowedImageTypes
            .find(_.value == contentType)
            .fold(Either.left[Failures, MediaType](UnsupportedImageType(contentType).single))(Either.right(_))
        }
        .getOrElse(Either.left(UnknownImageType.single))

    implicit object ImagePayloadUploader extends ImageUploader[ImagePayload] {

      def uploadImagesR(payload: ImagePayload, context: OC, maybeAlbum: Option[Album])(
          implicit ec: EC,
          db: DB,
          au: AU,
          am: Mat,
          sys: ActorSystem,
          apis: Apis): DbResultT[Seq[ImagePayload]] =
        for {
          url       ← * <~ validateImageUrl(payload.src)
          imageData ← * <~ fetchImageData(url)
          mediaType ← * <~ shouldBeValidImage(imageData)
          url ← * <~ saveBufferAndThen[String](imageData) { path ⇒
                 val fileName = extractFileNameFromUri(url)
                 val s3Path   = S3Path.get(maybeAlbum, fileName = fileName, mediaType = mediaType)
                 DbResultT.fromResult(apis.amazon.uploadFile(s3Path.absPath, path.toFile, overwrite = false))
               }
          newPayload = payload.copy(src = url)

          payloads ← * <~ attachImageTo(newPayload, context, maybeAlbum)

        } yield payloads.map(imageToPayload)
    }

    implicit object MultipartUploader extends ImageUploader[Multipart.FormData] {

      private def getImageFromRequest(
          bytes: Source[ByteString, Any])(implicit ec: EC, am: Mat): Future[Either[Failures, Path]] = {
        val file = Files.createTempFile("tmp", ".jpg")

        bytes.runWith(FileIO.toPath(file)).map { ioResult ⇒
          if (ioResult.wasSuccessful) Either.right(file)
          else Either.left(ErrorReceivingImage.single)
        }
      }

      private def uploadPathToS3(filePath: java.nio.file.Path,
                                 s3Path: S3Path)(implicit ec: EC, am: Mat, apis: Apis) =
        for {
          url ← apis.amazon.uploadFileF(s3Path.absPath, filePath.toFile, overwrite = false)
          _ = Files.deleteIfExists(filePath)
        } yield ImageUploaded(url = url, fileName = s3Path.fileName)

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
              filePathE ← getImageFromRequest(part.entity.dataBytes)
              filePath ← filePathE.fold(x ⇒ Future.failed[Path](ImageFacadeException(x.head)),
                                        Future.successful)
              fileName ← part.filename.fold(Future.failed[String](
                          ImageFacadeException(ImageFilenameNotFoundInPayload)))(Future.successful)
            } yield (filePath, fileName)
          }
          .mapAsyncUnordered(4) {
            case (filePath, fileName) ⇒
              shouldBeValidImage(filePath).fold(
                x ⇒ Future.failed[ImageUploaded](ImageFacadeException(x.head)), { mediaType ⇒
                  val s3path = S3Path.get(maybeAlbum, fileName = fileName, mediaType = mediaType)
                  uploadPathToS3(filePath, s3path)
                }
              )
          }
          .map { srcInfo ⇒
            val payload =
              ImagePayload(src = srcInfo.url, title = srcInfo.fileName.some, alt = srcInfo.fileName.some)

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

  }

  private def uploadImagesToAlbum[T](albumId: Int, contextName: String, imageSource: T)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      sys: ActorSystem,
      imageUploader: ImageUploader[T],
      apis: Apis): DbResultT[AlbumResponse] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ mustFindAlbumByFormIdAndContext404(albumId, context)
      _       ← * <~ album.mustNotBeArchived

      result ← * <~ imageUploader.uploadImagesToAlbum(album, context, imageSource)
    } yield result

  //+ endpoints
  def uploadImagesFromPayloadToAlbum(albumId: Int, contextName: String, payload: ImagePayload)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      sys: ActorSystem,
      apis: Apis): DbResultT[AlbumResponse] =
    uploadImagesToAlbum[ImagePayload](albumId, contextName, payload)

  def uploadImagesFromMultipartToAlbum(albumId: Int, contextName: String, formData: Multipart.FormData)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      sys: ActorSystem,
      apis: Apis): DbResultT[AlbumResponse] =
    uploadImagesToAlbum[Multipart.FormData](albumId, contextName, formData)

  def uploadImagesFromMultiPart(contextName: String, formData: Multipart.FormData)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      sys: ActorSystem,
      apis: Apis): DbResultT[Seq[ImagePayload]] =
    ImageUploader.MultipartUploader.uploadImages(formData, contextName)

  // - endpoints

  private def attachImageToAlbum(
      album: Album,
      payload: ImagePayload)(implicit ec: EC, db: DB, au: AU, oc: OC): DbResultT[Seq[FullObject[Image]]] =
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

trait ImageHelpers extends LazyLogging {

  protected def extractFileNameFromUri(uri: Uri): String = {

    @tailrec
    def latestSegment(path: Uri.Path): Option[String] =
      path match {
        case Uri.Path.Slash(tail)      ⇒ latestSegment(tail)
        case Uri.Path.Segment(head, _) ⇒ Some(head)
        case Uri.Path.Empty            ⇒ None
      }

    val revPath = uri.path.reverse
    latestSegment(revPath).getOrElse(s"$generateUuid.jpg")
  }

  def fetchImageData(uri: Uri)(implicit ec: EC, sys: ActorSystem, am: Mat): Result[ByteBuffer] = {
    val r = Http().singleRequest(HttpRequest(uri = uri)).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) ⇒
        entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body ⇒
          Either.right[Failures, ByteBuffer](body.asByteBuffer)
        }
      case HttpResponse(code, _, _, _) ⇒
        Future.successful(Either.left[Failures, ByteBuffer](ImageFetchFailed(code.intValue()).single))
    }

    Result.fromFEither(r)
  }

  private def saveByteBuffer(b: ByteBuffer): Either[Failures, Path] =
    Either
      .catchNonFatal {
        val path = Files.createTempFile("s3tmp", ".img")
        val ch   = FileChannel.open(path, java.nio.file.StandardOpenOption.WRITE)
        ch.write(b)
        ch.force(false)
        ch.close()
        path
      }
      .leftMap(ImageTemporarySaveFailed(_).single)

  protected def saveBufferAndThen[R](b: ByteBuffer)(block: Path ⇒ DbResultT[R])(
      implicit ec: EC): DbResultT[R] =
    for {
      path   ← * <~ saveByteBuffer(b)
      result ← * <~ block(path)
      _      ← * <~ Files.deleteIfExists(path)
    } yield result
}
