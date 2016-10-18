package payloads

import models.objects.ObjectUtils._
import models.objects.{FormAndShadow, ObjectForm, ObjectShadow}
import models.product.VariantValue
import payloads.ObjectPayloads._
import utils.aliases._

object VariantPayloads {
  case class VariantPayload(id: Option[Int] = None,
                            attributes: Map[String, Json],
                            values: Option[Seq[VariantValuePayload]],
                            scope: Option[String] = None)

  case class VariantValuePayload(id: Option[Int] = None,
                                 name: Option[String],
                                 swatch: Option[String],
                                 skuCodes: Seq[String],
                                 scope: Option[String] = None) {

    def formAndShadow: FormAndShadow = {
      val jsonBuilder: AttributesBuilder = ObjectPayloads
        .optionalAttributes(name.map(StringField("name", _)), swatch.map(StringField("swatch", _)))

      (ObjectForm(kind = VariantValue.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }
  }
}
