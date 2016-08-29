package facades

import java.nio.file.Files

import scala.concurrent.Future

import akka.http.scaladsl.model.{HttpRequest, Multipart}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString

import cats.data.Xor
import cats.implicits._
import failures.ImageFailures._
import models.image.{AlbumImageLinks, Image}
import models.objects.FullObject
import models.objects.ObjectContext
import payloads.ImagePayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import responses.AlbumResponses._
import services.Result
import services.image.ImageManager._
import services.objects.ObjectManager
import utils.aliases._
import utils.apis.Apis
import utils.db._
import utils.{IlluminateAlgorithm, JsonFormatters}

object ImageFacade {

  implicit def formats = JsonFormatters.phoenixFormats

  def uploadImage(
      albumId: Int,
      contextName: String,
      request: HttpRequest)(implicit ec: EC, db: DB, am: Mat, apis: Apis): Result[AlbumRoot] =
    Unmarshal(request.entity).to[Multipart.FormData].flatMap { formData ⇒
      val error: Result[AlbumRoot] = Result.failure(ImageNotFoundInPayload)
      ObjectManager.mustFindByName404(contextName).run().flatMap {
        case Xor.Right(context) ⇒
          formData.parts
            .filter(_.name == "upload-file")
            .runFold(error) { (_, part) ⇒
              (for {
                filePath ← * <~ getFileFromRequest(part.entity.dataBytes)
                filename ← * <~ getFileNameFromBodyPart(part)
                fullPath = s"albums/${context.id}/$albumId/$filename"
                url ← * <~ apis.amazon.uploadFile(fullPath, filePath.toFile)
              } yield (context, url, filename)).run().flatMap {
                case Xor.Right((context, url, filename)) ⇒
                  addImage(context, albumId, url, filename)
                case Xor.Left(err) ⇒
                  Result.left(err)
              }
            }
            .flatMap(a ⇒ a)
        case Xor.Left(err) ⇒ Result.left(err)
      }
    }

  private def addImage(context: ObjectContext, albumId: Int, url: String, filename: String)(
      implicit ec: EC,
      db: DB) = {
    (for {
      album          ← * <~ mustFindFullAlbumByFormIdAndContext404(albumId, context)
      _              ← * <~ album.model.mustNotBeArchived
      existingImages ← * <~ AlbumImageLinks.queryRightByLeft(album.model)
      payload = existingImages.map(imageToPayload) :+
        ImagePayload(src = url, title = filename.some, alt = filename.some)

      _           ← * <~ createOrUpdateImagesForAlbum(album.model, payload, context)
      albumImages ← * <~ AlbumImageLinks.queryRightByLeft(album.model)
    } yield AlbumResponse.build(album, albumImages)).runTxn()

  }

  private def getFileFromRequest(bytes: Source[ByteString, Any])(implicit ec: EC, am: Mat) = {
    val file = Files.createTempFile("tmp", ".jpg")
    bytes.runWith(FileIO.toPath(file)).map { ioResult ⇒
      if (ioResult.wasSuccessful) Xor.right(file)
      else Xor.left(ErrorReceivingImage.single)
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

    val src   = IlluminateAlgorithm.get("src", form, shadow).extract[String]
    val title = IlluminateAlgorithm.get("title", form, shadow).extractOpt[String]
    val alt   = IlluminateAlgorithm.get("alt", form, shadow).extractOpt[String]

    ImagePayload(Some(image.model.id), src, title, alt)
  }

}
