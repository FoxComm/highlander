package failures

object IlluminateFailures {

  final case class ShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Shadow has an invalid attribute $key with value $value"
  }
  final case class ShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Shadow attribute $key must be a string"
  }

  case object AttributesAreEmpty extends Failure {
    override def description = s"Form attributes are empty"
  }

  case object ShadowAttributesAreEmpty extends Failure {
    override def description = s"Shadow attributes are empty"
  }

}
