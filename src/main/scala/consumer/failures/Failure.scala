package consumer.failures

trait Failure {
  def description: String
}

case class GeneralFailure(a: String) extends Failure {
  override def description = a
}