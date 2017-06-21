package phoenix.payloads

import objectframework.ObjectUtils._
import objectframework.models.{FormAndShadow, ObjectForm, ObjectShadow}
import objectframework.payloads.ObjectPayloads
import objectframework.payloads.ObjectPayloads._
import phoenix.models.product.VariantValue
import phoenix.utils.aliases._

object VariantPayloads {
  case class VariantPayload(id: Option[Int] = None,
                            attributes: Map[String, Json],
                            values: Option[Seq[VariantValuePayload]],
                            schema: Option[String] = None,
                            scope: Option[String] = None)

  case class VariantValuePayload(id: Option[Int] = None,
                                 name: Option[String],
                                 swatch: Option[String],
                                 image: Option[String],
                                 skuCodes: Seq[String],
                                 schema: Option[String] = None,
                                 scope: Option[String] = None) {

    def formAndShadow: FormAndShadow = {
      val jsonBuilder: AttributesBuilder = ObjectPayloads.optionalAttributes(
        name.map(StringField("name", _)),
        swatch.map(StringField("swatch", _)),
        swatch.map(StringField("image", _)))

      (ObjectForm(kind = VariantValue.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }
  }
}
