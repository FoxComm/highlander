package facades

import java.nio.file.Files

import akka.http.scaladsl.model.{HttpRequest, Multipart}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString

import cats.data.Xor
import cats.implicits._
import failures.ImageFailures._
import payloads.ImagePayloads._
import responses.ImageResponses._
import services.Result
import services.image.ImageManager
import services.image.ImageManager._
import services.objects.ObjectManager
import utils.aliases._
import utils.apis.Apis
import utils.db._
import utils.{IlluminateAlgorithm, JsonFormatters}

object ImageFacade {

  implicit def formats = JsonFormatters.phoenixFormats

  def uploadImage(albumId: Int, contextName: String, request: HttpRequest)(
      implicit ec: EC,
      db: DB,
      am: Mat,
      apis: Apis): Result[AlbumResponse.Root] = {
    Unmarshal(request.entity)
      .to[Multipart.FormData]
      .flatMap { formData ⇒
        val error: Result[AlbumResponse.Root] = Result.failure(ImageNotFoundInPayload)
        formData.parts.filter(_.name == "upload-file").runFold(error) { (_, part) ⇒
          (for {
            context  ← * <~ ObjectManager.mustFindByName404(contextName)
            filePath ← * <~ getFileFromRequest(part.entity.dataBytes)
            album    ← * <~ ImageManager.mustFindFullAlbumByIdAndContext404(albumId, context)
            filename ← * <~ getFileNameFromBodyPart(part)
            fullPath ← * <~ s"albums/${context.id}/$albumId/$filename"
            s3       ← * <~ apis.amazon.uploadFile(fullPath, filePath.toFile)
            payload = payloadForNewImage(album, s3, filename)
            album ← * <~ updateAlbumInner(albumId, payload, contextName)
          } yield album).runTxn()
        }
      }
      .flatMap(a ⇒ a)
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

  private def payloadForNewImage(album: FullAlbum, imageUrl: String, filename: String) = {
    val formAttrs   = album.form.attributes
    val shadowAttrs = album.shadow.attributes

    val name           = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]
    val existingImages = IlluminateAlgorithm.get("images", formAttrs, shadowAttrs)
    val payload        = ImagePayload(src = imageUrl, title = filename.some, alt = filename.some)
    val imageSeq = existingImages.extractOpt[Seq[ImagePayload]].foldLeft(Seq(payload)) {
      (payload, existing) ⇒
        existing ++ payload
    }

    UpdateAlbumPayload(name = name.some, images = imageSeq.some)
  }

}
