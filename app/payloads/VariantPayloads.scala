package payloads

import models.objects.{ObjectForm, ObjectShadow}
import models.product.VariantValue
import payloads.ObjectPayloads.StringAttributePayload
import utils.aliases._

object VariantPayloads {
  case class CreateVariantPayload(
      attributes: Map[String, Json], values: Option[Seq[CreateVariantValuePayload]])

  case class CreateVariantValuePayload(name: String, swatch: Option[String]) {
    val nameAttribute   = StringAttributePayload("name", name)
    val swatchAttribute = swatch.map(s ⇒ StringAttributePayload("swatch", s))

    def objectForm: ObjectForm = {
      val json = swatchAttribute.foldLeft(nameAttribute.formJson)(_ merge _.formJson)
      ObjectForm(kind = VariantValue.kind, attributes = json)
    }

    val objectShadow: ObjectShadow = {
      val json = swatchAttribute.foldLeft(nameAttribute.shadowJson)(_ merge _.shadowJson)
      ObjectShadow(attributes = json)
    }
  }
}
