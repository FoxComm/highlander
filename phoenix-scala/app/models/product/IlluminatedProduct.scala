package models.product

import java.time.Instant

import failures.Failure
import failures.NotFoundFailure404
import models.objects._
import models.traits.IlluminatedModel
import utils.IlluminateAlgorithm
import utils.aliases._

/**
  * An IlluminatedProduct is what you get when you combine the product shadow and
  * the form.
  */
case class IlluminatedProduct(id: Int,
                              slug: String,
                              context: IlluminatedContext,
                              attributes: Json,
                              archivedAt: Option[Instant])
    extends IlluminatedModel[IlluminatedProduct] {

  override protected def inactiveError: Failure = NotFoundFailure404(Product, slug)

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
