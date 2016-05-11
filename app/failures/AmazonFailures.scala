package failures

object AmazonFailures {

  case object UnableToReadAwsAccessKey extends Failure {
    override def description = "Could not read AWS access key"
  }

  case object UnableToReadAwsSecretKey extends Failure {
    override def description = "Could not read AWS secret key"
  }

  case object UnableToReadAwsS3BucketName extends Failure {
    override def description = "Could not read AWS S3 bucket name"
  }
  case object UnableToReadAwsS3Region extends Failure {
    override def description = "Could not read AWS S3 region"
  }
}