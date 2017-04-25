package payloads

import io.circe.Json

object ObjectPayloads {
  type JsonField = (String, Json)

  trait FormShadowFieldBuilder {
    def formJson: JsonField
    def shadowJson: JsonField
  }

  class TypedFieldBuilder(attributeName: String, attributeType: String, attributeValue: Json)
      extends FormShadowFieldBuilder {
    override def formJson: JsonField = attributeName → attributeValue
    override def shadowJson: JsonField =
      attributeName                  → Json.obj("type" → Json.fromString(attributeType),
                               "ref" → Json.fromString(attributeName))
  }

  case class StringField(name: String, value: String)
      extends TypedFieldBuilder(name, "string", Json.fromString(value))
  case class IntField(name: String, value: Int)
      extends TypedFieldBuilder(name, "int", Json.fromInt(value))

  case class AttributesBuilder(attributes: FormShadowFieldBuilder*) {

    def objectForm: Json   = Json.obj(attributes.map(_.formJson): _*)
    def objectShadow: Json = Json.obj(attributes.map(_.shadowJson): _*)
  }

  def optionalAttributes(optional: Option[FormShadowFieldBuilder]*) =
    AttributesBuilder(optional.collect { case Some(field) ⇒ field }: _*)
}
