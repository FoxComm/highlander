package payloads

import cats.data._
import cats.implicits._
import failures.{Failure, Failures}
import failures.ProductFailures.DuplicatedOptionValueForVariant
import models.objects.ObjectUtils._
import models.objects.{FormAndShadow, ObjectForm, ObjectShadow}
import models.product.ProductOptionValue
import payloads.ObjectPayloads._
import utils.ValidatedPayload
import utils.aliases._

object ProductOptionPayloads {
  case class ProductOptionPayload(id: Option[Int] = None,
                                  attributes: Map[String, Json],
                                  values: Option[Seq[ProductOptionValuePayload]],
                                  schema: Option[String] = None,
                                  scope: Option[String] = None)
      extends ValidatedPayload[ProductOptionPayload] {
    def validate: ValidatedNel[Failure, ProductOptionPayload] = {
      val variantPerOptionValue = for {
        value ← values.getOrElse(Seq.empty)
        sku   ← value.skuCodes
      } yield sku → value
      val variantOptionValues = variantPerOptionValue.groupBy { case (code, _) ⇒ code }
      variantOptionValues.foldLeft(Validated.valid[Failures, ProductOptionPayload](this)) {
        case (acc, (sku, optionValues)) if optionValues.size > 1 ⇒
          (acc |@| Validated.invalidNel[Failure, ProductOptionPayload](
                  DuplicatedOptionValueForVariant(sku))).map {
            case (payload, _) ⇒ payload
          }
        case (acc, _) ⇒ acc
      }
    }
  }

  case class ProductOptionValuePayload(id: Option[Int] = None,
                                       name: Option[String],
                                       swatch: Option[String],
                                       skuCodes: Seq[String],
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
