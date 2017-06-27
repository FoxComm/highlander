package phoenix.utils.apis

import java.io.File

import scala.annotation.tailrec
import cats.implicits._
import com.amazonaws.AmazonClientException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{AmazonS3Exception, CannedAccessControlList, PutObjectRequest}
import com.typesafe.scalalogging.LazyLogging
import core.db._
import core.failures._
import phoenix.utils.FoxConfig.config
import java.net.URLEncoder

import scala.concurrent.Future

trait AmazonApi {

  def uploadFile(fileName: String, file: File, overwrite: Boolean)(implicit ec: EC): Result[String]
  def uploadFileF(fileName: String, file: File, overwrite: Boolean)(implicit ec: EC): Future[String]
}

object AmazonS3 {
  final implicit class URLEncodedString(val s: String) extends AnyVal {
    def urlEnc: String = URLEncoder.encode(s, "utf-8")
  }
}

class AmazonS3 extends AmazonApi with LazyLogging {
  import config.apis.aws._
  import AmazonS3._

  private[this] val credentials = new BasicAWSCredentials(accessKey, secretKey)
  private[this] val client      = new AmazonS3Client(credentials)

  @tailrec
  private def getAvailableObjectName(fileName: String, counter: Int = 0): String = {
    val newFileName = counter match {
      case 0 ⇒ fileName
      case _ ⇒
        fileName.lastIndexOf('.') match {
          case -1 ⇒ s"${fileName}_$counter"
          case dotIdx ⇒
            val ext = fileName.substring(dotIdx)
            s"${fileName.slice(0, dotIdx)}_$counter$ext"
        }
    }
    if (client.doesObjectExist(s3Bucket, newFileName)) {
      getAvailableObjectName(fileName, counter + 1)
    } else
      newFileName
  }

  def uploadFileF(fileName: String, file: File, overwrite: Boolean)(implicit ec: EC): Future[String] =
    Future {
      val objectName: String =
        if (overwrite) fileName
        else getAvailableObjectName(fileName)

      val putRequest = new PutObjectRequest(s3Bucket, objectName, file)
        .withCannedAcl(CannedAccessControlList.PublicRead)
      client.putObject(putRequest)

      s"https://s3-$s3Region.amazonaws.com/$s3Bucket/${objectName.urlEnc}"
    }

  def uploadFile(fileName: String, file: File, overwrite: Boolean)(implicit ec: EC): Result[String] = {
    val f = uploadFileF(fileName, file, overwrite).map(Either.right).recover {
      case e: AmazonS3Exception ⇒
        Either.left(GeneralFailure(e.getLocalizedMessage).single)
      case e: AmazonClientException ⇒
        logger.error(s"Can't upload file to AmazonS3", e)
        Either.left(GeneralFailure("An unexpected error occurred uploading to S3").single)
      case _ ⇒
        Either.left(GeneralFailure("An unexpected error occurred uploading to S3").single)
    }
    Result.fromFEither(f)
  }
}
