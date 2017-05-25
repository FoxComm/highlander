package phoenix.responses

import java.time.Instant

import cats.implicits._
import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.discount._
import phoenix.utils.aliases._

object DiscountResponses {

  object DiscountFormResponse {

    case class Root(id: Int, attributes: Json, createdAt: Instant)

    def build(f: ObjectForm): Root =
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object DiscountShadowResponse {

    case class Root(id: Int, attributes: Json, createdAt: Instant)

    //since shadow is always within some context, we will use the form id  for
    //id here
    def build(s: ObjectShadow): Root =
      Root(id = s.formId, attributes = s.attributes, createdAt = s.createdAt)
  }

  object DiscountResponse {
    case class Root(form: DiscountFormResponse.Root, shadow: DiscountShadowResponse.Root)

    def build(f: ObjectForm, s: ObjectShadow): Root =
      Root(form = DiscountFormResponse.build(f), shadow = DiscountShadowResponse.build(s))
  }

  object IlluminatedDiscountResponse {

    case class Root(id: Int, context: Option[ObjectContextResponse.Root], attributes: Json)

    def build(s: IlluminatedDiscount): Root =
      Root(id = s.id, context = s.context match {
        case Some(context) ⇒ ObjectContextResponse.build(context).some
        case None          ⇒ None
      }, attributes = s.attributes)
  }
}
