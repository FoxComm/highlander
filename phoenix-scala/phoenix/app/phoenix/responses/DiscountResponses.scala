package phoenix.responses

import java.time.Instant

import cats.implicits._
import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.discount._
import phoenix.utils.aliases._

object DiscountResponses {

  case class DiscountFormResponse(id: Int, attributes: Json, createdAt: Instant)

  object DiscountFormResponse {

    def build(f: ObjectForm): DiscountFormResponse =
      DiscountFormResponse(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  case class DiscountShadowResponse(id: Int, attributes: Json, createdAt: Instant)

  object DiscountShadowResponse {
    //since shadow is always within some context, we will use the form id for id here
    def build(s: ObjectShadow): DiscountShadowResponse =
      DiscountShadowResponse(id = s.formId, attributes = s.attributes, createdAt = s.createdAt)
  }

  case class DiscountResponse(form: DiscountFormResponse, shadow: DiscountShadowResponse)

  object DiscountResponse {

    def build(f: ObjectForm, s: ObjectShadow): DiscountResponse =
      DiscountResponse(form = DiscountFormResponse.build(f), shadow = DiscountShadowResponse.build(s))
  }

  case class IlluminatedDiscountResponse(id: Int, context: Option[ObjectContextResponse], attributes: Json)

  object IlluminatedDiscountResponse {

    def build(s: IlluminatedDiscount): IlluminatedDiscountResponse =
      IlluminatedDiscountResponse(id = s.id, context = s.context match {
        case Some(context) ⇒ ObjectContextResponse.build(context).some
        case None          ⇒ None
      }, attributes = s.attributes)
  }
}
