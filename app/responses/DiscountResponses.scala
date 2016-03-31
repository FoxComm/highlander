package responses

import models.objects._
import models.discount._
import models.Aliases.Json

import responses.ObjectResponses.ObjectContextResponse

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import java.time.Instant
import cats.implicits._

object DiscountResponses {

  object DiscountFormResponse {

    final case class Root(id: Int, attributes: Json, createdAt: Instant)

    def build(f: ObjectForm): Root = 
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object DiscountShadowResponse {

    final case class Root(id: Int, formId: Int, attributes: Json, createdAt: Instant)

    def build(s: ObjectShadow): Root = 
      Root(id = s.id, formId = s.formId, attributes = s.attributes, 
        createdAt = s.createdAt)
  }

  object DiscountResponse { 
    final case class Root(form: DiscountFormResponse.Root, shadow: DiscountShadowResponse.Root)

    def build(f: ObjectForm, s: ObjectShadow) : Root =
      Root(form = DiscountFormResponse.build(f), shadow = DiscountShadowResponse.build(s))
  }

  object IlluminatedDiscountResponse {

    final case class Root(id: Int, context: ObjectContextResponse.Root, attributes: Json)

    def build(s: IlluminatedDiscount): Root = 
      Root(id = s.id, context = ObjectContextResponse.build(s.context), 
        attributes = s.attributes)
  }
}
