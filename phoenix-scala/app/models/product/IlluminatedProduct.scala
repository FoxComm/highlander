package models.product

import java.time.Instant

import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._

/**
  * An IlluminatedProduct is what you get when you combine the product shadow and
  * the form.
  */
case class IlluminatedProduct(id: Int,
                              slug: Option[String],
                              context: IlluminatedContext,
                              attributes: Json,
                              archivedAt: Option[Instant])

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
