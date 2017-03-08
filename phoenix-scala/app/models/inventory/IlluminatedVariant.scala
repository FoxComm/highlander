package models.inventory

import java.time.Instant

import failures.{Failure, NotFoundFailure404}
import models.objects._
import models.traits.IlluminatedModel
import utils.IlluminateAlgorithm
import utils.aliases._

/**
  * An IlluminatedVariant is what you get when you combine the variant shadow and
  * the variant form.
  */
case class IlluminatedVariant(id: Int,
                              code: String,
                              archivedAt: Option[Instant],
                              context: IlluminatedContext,
                              attributes: Json)
    extends IlluminatedModel[IlluminatedVariant] {

  override protected def inactiveError: Failure = NotFoundFailure404(ProductVariant, id)

}

object IlluminatedVariant {

  def illuminate(context: ObjectContext, variant: FullObject[ProductVariant]): IlluminatedVariant = {
    val model       = variant.model
    val formAttrs   = variant.form.attributes
    val shadowAttrs = variant.shadow.attributes

    IlluminatedVariant(id = variant.form.id,
                       code = model.code,
                       archivedAt = model.archivedAt,
                       context = IlluminatedContext(context.name, context.attributes),
                       attributes = IlluminateAlgorithm.projectAttributes(formAttrs, shadowAttrs))
  }
}
