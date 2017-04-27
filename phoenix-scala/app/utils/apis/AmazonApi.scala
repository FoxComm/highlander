package utils.apis

import cats.implicits._
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{AmazonS3Exception, CannedAccessControlList, PutObjectRequest}
import failures._
import java.io.File
import scala.concurrent.Future
import utils.FoxConfig.config
import utils.aliases._
import utils.db._

trait AmazonApi {

  def uploadFile(fileName: String, file: File)(implicit ec: EC): Result[String]
}

class AmazonS3 extends AmazonApi {
  import config.apis.aws._

  private[this] val credentials = new BasicAWSCredentials(accessKey, secretKey)
  private[this] val client      = new AmazonS3Client(credentials)

  def uploadFile(fileName: String, file: File)(implicit ec: EC): Result[String] = {
    val f = Future {
      val putRequest = new PutObjectRequest(s3Bucket, fileName, file)
        .withCannedAcl(CannedAccessControlList.PublicRead)
      client.putObject(putRequest)
      Either.right(s"https://s3-$s3Region.amazonaws.com/$s3Bucket/$fileName")
    }.recover {
      case e: AmazonS3Exception ⇒
        Either.left(GeneralFailure(e.getLocalizedMessage).single)
      case _ ⇒
        Either.left(GeneralFailure("An unexpected error occurred uploading to S3").single)
    }
    Result.fromFEither(f)
  }
}
