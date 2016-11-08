package consumer.failures

trait Failure {
  def description: String
}

case class GeneralFailure(a: String) extends Failure {
  override def description = a
}

case class AvroDeserializeFailure(error: Throwable) extends Failure {
  override def description: String = s"Error serializing avro message $error"
}
