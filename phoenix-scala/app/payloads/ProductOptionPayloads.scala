package payloads

import models.objects.ObjectUtils._
import models.objects.{FormAndShadow, ObjectForm, ObjectShadow}
import models.product.ProductOptionValue
import payloads.ObjectPayloads._
import utils.aliases._

object ProductOptionPayloads {
  case class ProductOptionPayload(id: Option[Int] = None,
                                  attributes: Map[String, Json],
                                  values: Option[Seq[ProductOptionValuePayload]],
                                  schema: Option[String] = None,
                                  scope: Option[String] = None)

  case class ProductOptionValuePayload(id: Option[Int] = None,
                                       name: Option[String],
                                       swatch: Option[String],
                                       skus: Seq[String],
                                       schema: Option[String] = None,
                                       scope: Option[String] = None) {

    def formAndShadow: FormAndShadow = {
      val jsonBuilder: AttributesBuilder = ObjectPayloads
        .optionalAttributes(name.map(StringField("name", _)), swatch.map(StringField("swatch", _)))

      (ObjectForm(kind = ProductOptionValue.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }
  }
}
