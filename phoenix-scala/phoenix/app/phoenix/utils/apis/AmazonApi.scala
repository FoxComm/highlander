package phoenix.utils.apis

import java.io.File

import cats.implicits._
import com.amazonaws.AmazonClientException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{AmazonS3Exception, CannedAccessControlList, PutObjectRequest}
import com.typesafe.scalalogging.LazyLogging
import core.db._
import core.failures._
import phoenix.utils.FoxConfig.config

import scala.concurrent.Future

trait AmazonApi {

  def uploadFile(fileName: String, file: File)(implicit ec: EC): Result[String]
  def uploadFileF(fileName: String, file: File)(implicit ec: EC): Future[String]
}

class AmazonS3 extends AmazonApi with LazyLogging {
  import config.apis.aws._

  private[this] val credentials = new BasicAWSCredentials(accessKey, secretKey)
  private[this] val client      = new AmazonS3Client(credentials)

  def uploadFileF(fileName: String, file: File)(implicit ec: EC): Future[String] =
    Future {
      val putRequest = new PutObjectRequest(s3Bucket, fileName, file)
        .withCannedAcl(CannedAccessControlList.PublicRead)
      client.putObject(putRequest)
      s"https://s3-$s3Region.amazonaws.com/$s3Bucket/$fileName"
    }

  def uploadFile(fileName: String, file: File)(implicit ec: EC): Result[String] = {
    val f = uploadFileF(fileName, file).map(Either.right).recover {
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
