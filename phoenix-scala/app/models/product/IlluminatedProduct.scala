package models.product

import java.time.Instant

import cats.data.Xor
import cats.data.Xor.{Left, right}
import cats.implicits._
import failures.Failures
import failures.ProductFailures.ProductIsNotActive
import models.objects._
import utils.{IlluminateAlgorithm, JsonFormatters}
import utils.aliases._

/**
  * An IlluminatedProduct is what you get when you combine the product shadow and
  * the form.
  */
case class IlluminatedProduct(id: Int,
                              slug: String,
                              context: IlluminatedContext,
                              attributes: Json,
                              archivedAt: Option[Instant]) {

  implicit val formats = JsonFormatters.phoenixFormats

  def mustBeActive: Failures Xor IlluminatedProduct = {
    val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]
    val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
    val now        = Instant.now

    (activeFrom, activeTo) match {
      case (Some(from), Some(to)) ⇒
        if (from.isBefore(now) && to.isAfter(now)) right(this)
        else Left(ProductIsNotActive(ProductReference(slug)).single)
      case (Some(from), None) ⇒
        if (from.isBefore(now)) right(this)
        else Left(ProductIsNotActive(ProductReference(slug)).single)
      case _ ⇒
        Left(ProductIsNotActive(ProductReference(slug)).single)
    }
  }
}

object IlluminatedProduct {

  def illuminate(context: ObjectContext,
                 product: Product,
                 form: ObjectForm,
                 shadow: ObjectShadow): IlluminatedProduct = {

    IlluminatedProduct(id = form.id,
                       slug = product.slug,
                       context = IlluminatedContext(context.name, context.attributes),
                       attributes =
                         IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes),
                       archivedAt = product.archivedAt)
  }
}
