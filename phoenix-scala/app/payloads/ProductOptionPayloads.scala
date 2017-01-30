package payloads

import cats.data._
import cats.implicits._
import failures.{Failure, GeneralFailure}
import models.objects.ObjectUtils._
import models.objects.{FormAndShadow, ObjectForm, ObjectShadow}
import models.product.ProductOptionValue
import payloads.ObjectPayloads._
import utils.{ValidatedPayload, Validation}
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
        code  ← value.skuCodes
      } yield code → value
      val variantOptionValues = variantPerOptionValue.groupBy(_._1).iterator
      variantOptionValues.foldLeft(
          Validated.valid[NonEmptyList[Failure], ProductOptionPayload](this)) {
        case (acc, (code, optionValues)) ⇒
          if (optionValues.size > 1)
            (acc |@| Validated.invalidNel[Failure, ProductOptionPayload](GeneralFailure(
                        s"Variant $code has multiple option values attached: ${optionValues
                  .map(_._2.name.getOrElse("Nameless"))
                  .mkString(", ")}"))).map { case (p, _) ⇒ p } else
            acc
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
