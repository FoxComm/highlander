package payloads

import cats.data._
import cats.implicits._
import failures.ProductFailures.DuplicatedOptionValueForVariant
import failures.{Failure, Failures}
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
                                  scope: Option[String] = None) {
    def validate: ValidatedNel[Failure, ProductOptionPayload] = {

      val variantPerOptionValue = for {
        value ← values.getOrElse(Seq.empty)
        sku   ← value.skus
      } yield sku → value

      val valid = Validated.valid[Failures, ProductOptionPayload](this)

      def invalid(sku: String) =
        Validated.invalidNel[Failure, ProductOptionPayload](DuplicatedOptionValueForVariant(sku))

      variantPerOptionValue.groupBy { case (code, _) ⇒ code }.foldLeft(valid) {
        case (acc, (sku, optionValues)) if optionValues.size > 1 ⇒
          (acc |@| invalid(sku)).map {
            case (payload, _) ⇒ payload
          }
        case (acc, _) ⇒ acc
      }
    }
  }

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
