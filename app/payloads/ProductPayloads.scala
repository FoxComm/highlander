package payloads

import cats.data._
import cats.implicits._
import payloads.SkuPayloads._
import failures.Failure
import payloads.VariantPayloads.VariantPayload
import utils.Validation
import utils.Validation._
import utils.aliases._

object ProductPayloads {
  case class CreateProductPayload(attributes: Map[String, Json],
                                  skus: Seq[SkuPayload],
                                  variants: Option[Seq[VariantPayload]])
      extends Validation[CreateProductPayload] {

    def validate: ValidatedNel[Failure, CreateProductPayload] = {
      val maxSkus = variants match {
        case Some(variantPayloads) ⇒
          variantPayloads.foldLeft(1) { (acc, payload) ⇒
            acc * payload.values.map(_.length).getOrElse(1)
          }
        case None ⇒
          1
      }

      (notEmpty(skus, "SKUs") |@| lesserThanOrEqual(skus.length, maxSkus, "number of SKUs")).map {
        case _ ⇒ this
      }
    }
  }

  case class UpdateProductPayload(attributes: Map[String, Json],
                                  skus: Option[Seq[SkuPayload]],
                                  variants: Option[Seq[VariantPayload]])
}
