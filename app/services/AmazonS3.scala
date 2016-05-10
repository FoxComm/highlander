package services

import java.io.File

import scala.concurrent.{Future, blocking}

import cats.data.Xor.{left, right}
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.AmazonS3Exception
import failures._
import utils.aliases.EC
import utils.FoxConfig.{RichConfig, config}

object AmazonS3 {
  def uploadFile(fileName: String, file: File)(implicit ec: EC): Result[String] = Future {
    val accessKey = config.getOptString("aws.accessKey")
    val secretKey = config.getOptString("aws.secretKey")
    val s3Bucket = config.getOptString("aws.bucket")
    val s3Region = config.getOptString("aws.region")

    (accessKey, secretKey, s3Bucket, s3Region) match {
      case (Some(access), Some(secret), Some(bucket), Some(region)) ⇒
        val credentials = new BasicAWSCredentials(access, secret)
        val client = new AmazonS3Client(credentials)
        client.putObject(bucket, fileName, file)
        right(s"https://s3-${region}.amazonaws.com/${bucket}/${fileName}")
      case (None, _, _, _) ⇒
        left(GeneralFailure("Could not read AWS access key").single)
      case (_, None, _, _) ⇒
        left(GeneralFailure("Could not read AWS secret key").single)
      case (_, _, None, _) ⇒
        left(GeneralFailure("Could not read S3 bucket").single)
      case (_, _, _, None) ⇒
        left(GeneralFailure("Count not read S3 region").single)
    }
  }.recoverWith {
    case e: AmazonS3Exception ⇒
      Result.failure(GeneralFailure(e.getLocalizedMessage))
    case _ ⇒
      Result.failure(GeneralFailure("An unexpected error occurred uploading to S3"))
  }
}
