package failures

object ObjectFailures {

  final case class ObjectContextNotFound(name: String) extends Failure {
    override def description = s"Context with name $name cannot be found"
  }

  final case class ShadowAttributeMissingRef(name: String) extends Failure {
    override def description = s"Shadow attribute ref $name is missing from form"
  }

  final case class ShadowHasInvalidAttribute(attr: String, key: String) extends Failure {
    override def description = s"Cannot find attribute $attr with key $key in form"
  }

  final case class AttributesAreEmpty() extends Failure {
    override def description = s"Form attributes are empty"
  }

  final case class ShadowAttributesAreEmpty() extends Failure {
    override def description = s"Shadow attributes are empty"
  }

  final case class ObjectLinkCannotBeFound(left: Int, right: Int) extends Failure {
    override def description = s"Object link $left ⇒ $right cannot be found"
  }

  final case class ObjectLeftLinkCannotBeFound(left: Int) extends Failure {
    override def description = s"Object link with left id $left cannot be found"
  }


}
