package phoenix.failures

import core.failures.Failure

object IlluminateFailures {

  case class ShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Shadow has an invalid attribute $key with value $value"
  }
  case class ShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Shadow attribute $key must be a string"
  }

  case object AttributesAreEmpty extends Failure {
    override def description = "Form attributes are empty"
  }

  case object ShadowAttributesAreEmpty extends Failure {
    override def description = "Shadow attributes are empty"
  }
}
