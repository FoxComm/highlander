package facades

import java.nio.ByteBuffer
import java.io.{File, FileOutputStream}
import java.nio.channels.FileChannel
import java.nio.file.{Files, Path}

import akka.http.scaladsl.model.{HttpRequest, Multipart}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import scala.collection.immutable.{Seq ⇒ ImmutableSeq}

import cats.implicits._
import failures.Failures
import failures.ImageFailures._
import java.nio.file.Files

import models.objects.FullObject
import payloads.ImagePayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import scala.concurrent.Future

import responses.ImageResponses._
import services.image.ImageManager._
import services.objects.ObjectManager
import utils.aliases._
import utils.apis.Apis
import utils.db._
import utils.{IlluminateAlgorithm, JsonFormatters}
import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.{FromResponseUnmarshaller, Unmarshal, Unmarshaller}
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, Materializer}
import concurrent.duration._

import com.typesafe.scalalogging.LazyLogging

object ImageFacade extends LazyLogging {
  implicit val system: ActorSystem             = ActorSystem.create("Images")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  case class UnableToFetchImage(code: Int) extends Throwable

  implicit def formats = JsonFormatters.phoenixFormats

  private def extractFileNameFromUrl(url: String): String = {
    val i = url.lastIndexOf('/')
    if (i > 0)
      url.substring(i + 1)
    else
      s"${utils.generateUuid}.jpg"
  }

  private def fetchImageData(url: String)(implicit ec: EC): Result[ByteBuffer] = {
    val r = Http().singleRequest(HttpRequest(uri = url)).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) ⇒
        entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body ⇒
          Either.right[Failures, ByteBuffer](body.asByteBuffer)
        }
      case HttpResponse(code, _, _, _) ⇒
        Future.successful(
            Either.left[Failures, ByteBuffer](ImageFetchFailed(code.intValue()).single))
    }

    Result.fromFEither(r)
  }

  private def saveByteBuffer(b: ByteBuffer): Path = {
    val path = Files.createTempFile("s3tmp", ".img")
    val ch   = FileChannel.open(path, java.nio.file.StandardOpenOption.WRITE)
    ch.write(b)
    ch.force(false)
    ch.close()
    path
  }

  def uploadImageToS3(payload: UploadImageByUrlPayload)(implicit ec: EC,
                                                        db: DB,
                                                        apis: Apis): Result[S3ImageResponse] = {
    for {
      byteBuffer ← fetchImageData(payload.url)
      path = saveByteBuffer(byteBuffer)
      url ← apis.amazon.uploadFile(extractFileNameFromUrl(payload.url), path.toFile)
    } yield S3ImageResponse(s3url = url)
  }
}
