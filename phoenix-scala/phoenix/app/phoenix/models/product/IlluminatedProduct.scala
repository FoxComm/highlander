package phoenix.models.product

import java.time.Instant

import failures.{Failure, NotFoundFailure404}
import models.objects._
import phoenix.models.traits.IlluminatedModel
import phoenix.utils.aliases._
import utils.IlluminateAlgorithm

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

  override def inactiveError: Failure = NotFoundFailure404(Product, slug)

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
