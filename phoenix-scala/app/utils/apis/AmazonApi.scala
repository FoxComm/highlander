package utils.apis

import java.io.File

import scala.concurrent.Future

import cats.data.Xor.{left, right}
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{AmazonS3Exception, CannedAccessControlList, PutObjectRequest}
import failures._
import services.Result
import utils.FoxConfig.config
import utils.aliases._

trait AmazonApi {

  def uploadFile(fileName: String, file: File)(implicit ec: EC): Result[String]
}

class AmazonS3 extends AmazonApi {
  def uploadFile(fileName: String, file: File)(implicit ec: EC): Result[String] =
    Future {
      import config.apis.aws._

      val credentials = new BasicAWSCredentials(accessKey, secretKey)
      val client      = new AmazonS3Client(credentials)
      val putRequest = new PutObjectRequest(s3Bucket, fileName, file)
        .withCannedAcl(CannedAccessControlList.PublicRead)
      client.putObject(putRequest)
      right(s"https://s3-$s3Region.amazonaws.com/$s3Bucket/$fileName")
    }.recoverWith {
      case e: AmazonS3Exception ⇒
        Result.failure(GeneralFailure(e.getLocalizedMessage))
      case _ ⇒
        Result.failure(GeneralFailure("An unexpected error occurred uploading to S3"))
    }
}
