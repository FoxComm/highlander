package facades

import akka.http.scaladsl.model.{HttpRequest, Multipart}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import cats.implicits._
import failures.Failures
import failures.ImageFailures._
import java.nio.file.Files
import models.image._
import models.objects.FullObject
import payloads.ImagePayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import scala.concurrent.Future
import services.image.ImageManager._
import services.objects.ObjectManager
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.apis.Apis
import utils.db._
import utils.json.yolo._

object ImageFacade {

  def uploadImages(albumId: Int, contextName: String, request: HttpRequest)(
      implicit ec: EC,
      db: DB,
      au: AU,
      am: Mat,
      apis: Apis): Result[AlbumRoot] = {
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ mustFindAlbumByFormIdAndContext404(albumId, context)
      _       ← * <~ album.mustNotBeArchived
      result  ← * <~ uploadImages(album, request, context)
    } yield result).runDBIO()
  }

  def uploadImages(
      album: Album,
      request: HttpRequest,
      context: OC)(implicit ec: EC, db: DB, au: AU, am: Mat, apis: Apis): Result[AlbumRoot] = {
    val failures                              = ImageNotFoundInPayload.single
    val error: Future[Either[Failures, Unit]] = Future.successful(Either.left(failures))
    implicit val oc                           = context

    // FIXME: this needs a rewrite badly. No .runTxn, runDBIO or .runEmptyA, or .value should be here. @michalrus
    // FIXME: Naive approach at composition fails with Akka’s SubscriptionTimeoutException.
    // FIXME: Investigate Akka’s `runFold` maybe?

    val xs = Unmarshal(request.entity).to[Multipart.FormData].flatMap { formData ⇒
      formData.parts
        .filter(_.name == "upload-file")
        .runFold(error) { (previousUpload, part) ⇒
          previousUpload.flatMap {
            case Left(err) if err != failures ⇒ Future.successful(Either.left(err))
            case _                            ⇒ uploadImage(part, album).runTxn().runEmptyA.value
          }
        }
        .flatMap { r ⇒
          (for {
            _     ← * <~ r
            album ← * <~ getAlbumInner(album.formId, oc)
          } yield album).runDBIO().runEmptyA.value
        }
    }

    Result.fromFEither(xs)
  }

  def uploadImage(part: Multipart.FormData.BodyPart, album: Album)(implicit ec: EC,
                                                                   db: DB,
                                                                   oc: OC,
                                                                   au: AU,
                                                                   mat: Mat,
                                                                   apis: Apis): DbResultT[Unit] =
    for {
      filePath ← * <~ getFileFromRequest(part.entity.dataBytes)
      fileName ← * <~ getFileNameFromBodyPart(part)
      fullPath = s"albums/${oc.id}/${album.formId}/$fileName"

      url ← * <~ apis.amazon.uploadFile(fullPath, filePath.toFile)

      existingImages ← * <~ AlbumImageLinks.queryRightByLeft(album)
      payload = existingImages.map(imageToPayload) :+
        ImagePayload(src = url, title = fileName.some, alt = fileName.some)

      _ ← * <~ createOrUpdateImagesForAlbum(album, payload, oc)
    } yield {}

  private def getFileFromRequest(bytes: Source[ByteString, Any])(implicit ec: EC, am: Mat) = {
    val file = Files.createTempFile("tmp", ".jpg")
    bytes.runWith(FileIO.toPath(file)).map { ioResult ⇒
      if (ioResult.wasSuccessful) Either.right(file)
      else Either.left(ErrorReceivingImage.single)
    }
  }

  private def getFileNameFromBodyPart(part: Multipart.FormData.BodyPart)(implicit ec: EC) = {
    part.filename match {
      case Some(fileName) ⇒ Result.good(fileName)
      case None           ⇒ Result.failure(ImageFilenameNotFoundInPayload)
    }
  }

  private def imageToPayload(image: FullObject[Image]): ImagePayload = {
    val form   = image.form.attributes
    val shadow = image.shadow.attributes

    val src   = IlluminateAlgorithm.get("src", form, shadow).orEmpty.extract[String]
    val title = IlluminateAlgorithm.get("title", form, shadow).flatMap(_.asString)
    val alt   = IlluminateAlgorithm.get("alt", form, shadow).flatMap(_.asString)

    ImagePayload(Some(image.model.id), src, title, alt)
  }

}
