package utils.apis

import java.io.File

import scala.concurrent.Future

import cats.data.Xor.{left, right}
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{AmazonS3Exception, CannedAccessControlList, PutObjectRequest}
import failures.AmazonFailures._
import failures._
import services.Result
import utils.FoxConfig.{RichConfig, config}
import utils.aliases._

trait AmazonApi {

  def uploadFile(fileName: String, file: File)(implicit ec: EC): Result[String]
}

class AmazonS3 extends AmazonApi {
  def uploadFile(fileName: String, file: File)(implicit ec: EC): Result[String] = {
    val f = Future {
      val accessKey = config.getOptString("aws.accessKey")
      val secretKey = config.getOptString("aws.secretKey")
      val s3Bucket = config.getOptString("aws.s3Bucket")
      val s3Region = config.getOptString("aws.s3Region")

      (accessKey, secretKey, s3Bucket, s3Region) match {
        case (Some(access), Some(secret), Some(bucket), Some(region)) ⇒
          val credentials = new BasicAWSCredentials(access, secret)
          val client = new AmazonS3Client(credentials)
          val putRequest = new PutObjectRequest(bucket, fileName, file)
            .withCannedAcl(CannedAccessControlList.PublicRead)
          client.putObject(putRequest)
          right(s"https://s3-$region.amazonaws.com/$bucket/$fileName")
        case (None, _, _, _) ⇒
          left(UnableToReadAwsAccessKey.single)
        case (_, None, _, _) ⇒
          left(UnableToReadAwsSecretKey.single)
        case (_, _, None, _) ⇒
          left(UnableToReadAwsS3BucketName.single)
        case (_, _, _, None) ⇒
          left(UnableToReadAwsS3Region.single)
      }
    }.recover {
      case e: AmazonS3Exception ⇒
        left(GeneralFailure(e.getLocalizedMessage).single)
      case _ ⇒
        left(GeneralFailure("An unexpected error occurred uploading to S3").single)
    }
    Result.fromFutureXor(f)
  }
}
