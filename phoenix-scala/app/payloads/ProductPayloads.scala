package payloads

import cats.data.Validated.valid
import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductOptionPayloads.ProductOptionPayload
import payloads.ProductVariantPayloads._
import utils.aliases._
import utils.db._

object ProductPayloads {
  case class CreateProductPayload(scope: Option[String] = None,
                                  attributes: Map[String, Json],
                                  slug: String = "",
                                  variants: Seq[ProductVariantPayload],
                                  options: Option[Seq[ProductOptionPayload]],
                                  albums: Option[Seq[AlbumPayload]] = None,
                                  override val schema: Option[String] = None)
      extends ObjectSchemaValidation.SchemaValidation[CreateProductPayload] {

    override def defaultSchemaName: String = "product"

    override def validate(implicit ec: EC): DbResultT[CreateProductPayload] = {
      val thisValid: ValidatedNel[Failure, CreateProductPayload] = valid(this)
      for {
        _ ← * <~ options.fold(thisValid)(_.foldLeft(thisValid) { (validationAcc, optionPayload) ⇒
             (validationAcc |@| optionPayload.validate).map { case _ ⇒ this }
           })
        _ ← * <~ super.validate
      } yield this
    }
  }

  case class UpdateProductPayload(attributes: Map[String, Json],
                                  slug: Option[String] = None,
                                  variants: Option[Seq[ProductVariantPayload]],
                                  options: Option[Seq[ProductOptionPayload]],
                                  albums: Option[Seq[AlbumPayload]] = None)
      extends ObjectSchemaValidation.SchemaValidation[UpdateProductPayload] {
    override def defaultSchemaName: String = "product"
  }

}
