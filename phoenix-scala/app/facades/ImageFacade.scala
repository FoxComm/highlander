package facades

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Files, Path}

import scala.collection.immutable.{Seq ⇒ ImmutableSeq}
import scala.concurrent.Future
import scala.util.{Success, Try}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.stream.ActorMaterializer
import akka.util.ByteString

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import failures.Failures
import failures.ImageFailures._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import utils.aliases._
import utils.db._

trait ImageFacade extends LazyLogging {
  implicit val system: ActorSystem             = ActorSystem.create("Images")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  case class UnableToFetchImage(code: Int) extends Throwable

  protected def extractFileNameFromUrl(url: String): String = {
    val i = url.lastIndexOf('/')
    if (i > 0)
      url.substring(i + 1)
    else
      s"${utils.generateUuid}.jpg"
  }

  def fetchImageData(url: String)(implicit ec: EC): Result[ByteBuffer] = {
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

  private def saveByteBuffer(b: ByteBuffer): Either[Failures, Path] = {
    Try {
      val path = Files.createTempFile("s3tmp", ".img")
      val ch   = FileChannel.open(path, java.nio.file.StandardOpenOption.WRITE)
      ch.write(b)
      ch.force(false)
      ch.close()
      path
    } match { // TODO: .toEither in scala 2.12 @narma
      case Success(path)         ⇒ Either.right[Failures, Path](path)
      case scala.util.Failure(e) ⇒ Either.left[Failures, Path](ImageTemporarySaveFailed(e).single)
    }
  }

  protected def saveBufferAndThen[R](b: ByteBuffer)(block: Path ⇒ DbResultT[R])(
      implicit ec: EC): DbResultT[R] =
    for {
      path   ← * <~ saveByteBuffer(b)
      result ← * <~ block(path)
      _      ← * <~ Files.deleteIfExists(path)
    } yield result

}
