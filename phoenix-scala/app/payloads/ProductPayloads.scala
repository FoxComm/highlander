package payloads

import cats.data.Validated.valid
import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductOptionPayloads.ProductOptionPayload
import payloads.ProductVariantPayloads._
import utils.Validation
import utils.aliases._

object ProductPayloads {
  case class CreateProductPayload(scope: Option[String] = None,
                                  attributes: Map[String, Json],
                                  slug: String = "",
                                  variants: Seq[ProductVariantPayload],
                                  options: Option[Seq[ProductOptionPayload]],
                                  albums: Option[Seq[AlbumPayload]] = None,
                                  schema: Option[String] = None)
      extends Validation[CreateProductPayload] {

    override def validate: ValidatedNel[Failure, CreateProductPayload] = {
      val thisValid: ValidatedNel[Failure, CreateProductPayload] = valid(this)

      options.fold(thisValid)(_.foldLeft(thisValid) { (validationAcc, optionPayload) ⇒
        (validationAcc |@| optionPayload.validate).map { case _ ⇒ this }
      })
    }.map { case _ ⇒ this }
  }

  case class UpdateProductPayload(attributes: Map[String, Json],
                                  slug: Option[String] = None,
                                  variants: Option[Seq[ProductVariantPayload]],
                                  options: Option[Seq[ProductOptionPayload]],
                                  albums: Option[Seq[AlbumPayload]] = None)
}
