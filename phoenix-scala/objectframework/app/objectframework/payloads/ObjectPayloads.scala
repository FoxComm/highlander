package objectframework.payloads

import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s.{JField, JValue}

object ObjectPayloads {
  trait FormShadowFieldBuilder {
    def formJson: JField
    def shadowJson: JField
  }

  class TypedFieldBuilder(attributeName: String, attributeType: String, attributeValue: JValue)
      extends FormShadowFieldBuilder {
    override def formJson: JField = attributeName → attributeValue
    override def shadowJson: JField =
      attributeName → (("type" → attributeType) ~ ("ref" → attributeName))
  }

  case class StringField(name: String, value: String) extends TypedFieldBuilder(name, "string", value)
  case class IntField(name: String, value: Int)       extends TypedFieldBuilder(name, "int", value)

  case class AttributesBuilder(attributes: FormShadowFieldBuilder*) {

    def objectForm   = JObject(attributes.map(_.formJson): _*)
    def objectShadow = JObject(attributes.map(_.shadowJson): _*)
  }

  def optionalAttributes(optional: Option[FormShadowFieldBuilder]*) =
    AttributesBuilder(optional.collect { case Some(field) ⇒ field }: _*)
}
