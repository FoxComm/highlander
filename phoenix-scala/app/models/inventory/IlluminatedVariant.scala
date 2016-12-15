package models.inventory

import java.time.Instant

import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._

/**
  * An IlluminatedVariant is what you get when you combine the productOption shadow and
  * the productOption.
  */
case class IlluminatedVariant(id: Int,
                              code: String,
                              archivedAt: Option[Instant],
                              context: IlluminatedContext,
                              attributes: Json)

object IlluminatedVariant {

  def illuminate(context: ObjectContext, sku: FullObject[ProductVariant]): IlluminatedVariant = {
    val model       = sku.model
    val formAttrs   = sku.form.attributes
    val shadowAttrs = sku.shadow.attributes

    IlluminatedVariant(id = sku.form.id,
                       code = model.code,
                       archivedAt = model.archivedAt,
                       context = IlluminatedContext(context.name, context.attributes),
                       attributes = IlluminateAlgorithm.projectAttributes(formAttrs, shadowAttrs))
  }
}
