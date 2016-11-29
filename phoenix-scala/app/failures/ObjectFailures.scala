package failures

import models.objects.ObjectForm

object ObjectFailures {

  object ObjectContextNotFound {
    def apply(name: String): NotFoundFailure404 =
      NotFoundFailure404(s"Context with name $name cannot be found")
  }

  case class ObjectValidationFailure(kind: String, id: Int, errors: String) extends Failure {
    override def description = s"Object $kind with id=$id doesn't pass validation: $errors"
  }

  case class PayloadValidationFailure(error: String) extends Failure {
    override def description: String = s"Payload validation failed: $error"
  }

  case class ShadowAttributeMissingRef(name: String) extends Failure {
    override def description = s"Shadow attribute ref $name is missing from form"
  }

  case class ShadowAttributeInvalidTime(attr: String, value: String) extends Failure {
    override def description = s"Shadow attribute $attr contains invalid time value $value"
  }

  case class ShadowHasInvalidAttribute(attr: String, key: String) extends Failure {
    override def description = s"Cannot find attribute $attr with key $key in form"
  }

  case object FormAttributesAreEmpty extends Failure {
    override def description = "Form attributes are empty"
  }

  case object ShadowAttributesAreEmpty extends Failure {
    override def description = "Shadow attributes are empty"
  }

  case class LinkAtPositionCannotBeFound(clazz: Class[_], left: Int, position: Int)
      extends Failure {
    override def description =
      s"No object link ${clazz.getSimpleName} with left id $left exists at position $position"
  }

  case class LinkCannotBeFound(clazz: Class[_], left: Int, right: Int) extends Failure {
    override def description = s"Object link ${clazz.getSimpleName} $left ⇒ $right cannot be found"
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

  case object ObjectHeadCannotBeFoundByFormId {
    def apply(tableName: String, formId: ObjectForm#Id, contextName: String) =
      NotFoundFailure404(
          s"Object '$tableName' with id $formId cannot be found in context '$contextName'")
  }
}
