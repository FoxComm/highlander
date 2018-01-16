package objectframework.payloads

import cats.data.{Validated, ValidatedNel}
import core.failures.Failure
import core.utils.Validation
import core.utils.Validation._
import objectframework.content._

object ContentPayloads {
  // TODO: At some point, make this is a trait so that we can implement payloads
  // with this contract that can be understood by ContentManager.
  case class CreateContentPayload(kind: String,
                                  attributes: Content.ContentAttributes,
                                  relations: Content.ContentRelations)
      extends Validation[CreateContentPayload] {

    override def validate: ValidatedNel[Failure, CreateContentPayload] =
      validExpr(kind.trim.nonEmpty, "Kind is empty").map(_ ⇒ this)
  }

  case class UpdateContentPayload(attributes: Option[Content.ContentAttributes],
                                  relations: Option[Content.ContentRelations])
      extends Validation[UpdateContentPayload] {

    override def validate: ValidatedNel[Failure, UpdateContentPayload] =
      validExpr(attributes.nonEmpty || relations.nonEmpty, "Payload has no data").map(_ ⇒ this)
  }
}
