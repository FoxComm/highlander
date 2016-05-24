package failures

object ObjectFailures {

  case class ObjectContextNotFound(name: String) extends Failure {
    override def description = s"Context with name $name cannot be found"
  }

  case class ObjectFormNotFound(id: Int) extends Failure {
    override def description = s"Form with id $id cannot be found"
  }

  case class ObjectShadowNotFound(id: Int) extends Failure {
    override def description = s"Shadow with id $id cannot be found"
  }

  case class ShadowAttributeMissingRef(name: String) extends Failure {
    override def description = s"Shadow attribute ref $name is missing from form"
  }

  case class ShadowHasInvalidAttribute(attr: String, key: String) extends Failure {
    override def description = s"Cannot find attribute $attr with key $key in form"
  }

  case object AttributesAreEmpty extends Failure {
    override def description = "Form attributes are empty"
  }

  case object ShadowAttributesAreEmpty extends Failure {
    override def description = "Shadow attributes are empty"
  }

  case class ObjectLinkCannotBeFound(left: Int, right: Int) extends Failure {
    override def description = s"Object link $left ⇒ $right cannot be found"
  }

  case class ObjectLeftLinkCannotBeFound(left: Int) extends Failure {
    override def description = s"Object link with left id $left cannot be found"
  }

  case class ObjectRightLinkCannotBeFound(right: Int) extends Failure {
    override def description = s"Object link with right id $right cannot be found"
  }
}
