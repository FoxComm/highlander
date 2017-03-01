package payloads

import cats.data._
import failures.Failure
import failures.ProductFailures.DuplicatedOptionValueForVariant
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

      val failures = variantPerOptionValue.groupBy { case (sku, _) ⇒ sku }.collect {
        case (sku, dupes) if dupes.size > 1 ⇒ DuplicatedOptionValueForVariant(sku)
      }

      failures match {
        case Nil     ⇒ Validated.valid(this)
        case x :: xs ⇒ Validated.invalid(NonEmptyList.of(x, xs: _*))
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
