package payloads

import models.objects.{ObjectForm, ObjectShadow}
import models.product.VariantValue
import org.json4s.JsonAST.{JNothing, JValue}
import payloads.ObjectPayloads.StringAttributePayload
import utils.aliases._

object VariantPayloads {
  case class VariantPayload(id: Option[Int] = None,
                            attributes: Map[String, Json],
                            values: Option[Seq[VariantValuePayload]])

  case class VariantValuePayload(
      id: Option[Int] = None, name: Option[String], swatch: Option[String]) {
    val nameAttribute   = name.map(n ⇒ StringAttributePayload("name", n))
    val swatchAttribute = swatch.map(s ⇒ StringAttributePayload("swatch", s))

    val objectForm: ObjectForm = {
      val nameJson = nameAttribute.foldLeft(JNothing: JValue)(_ merge _.formJson)
      val json     = swatchAttribute.foldLeft(nameJson)(_ merge _.formJson)
      ObjectForm(kind = VariantValue.kind, attributes = json)
    }

    val objectShadow: ObjectShadow = {
      val nameJson = nameAttribute.foldLeft(JNothing: JValue)(_ merge _.shadowJson)
      val json     = swatchAttribute.foldLeft(nameJson)(_ merge _.shadowJson)
      ObjectShadow(attributes = json)
    }
  }
}
