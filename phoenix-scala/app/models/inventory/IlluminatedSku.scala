package models.inventory

import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._

/**
  * An IlluminatedSku is what you get when you combine the sku shadow and
  * the sku. 
  */
case class IlluminatedSku(id: Int, code: String, context: IlluminatedContext, attributes: Json)

object IlluminatedSku {

  def illuminate(context: ObjectContext, sku: FullObject[Sku]): IlluminatedSku = {
    val model       = sku.model
    val formAttrs   = sku.form.attributes
    val shadowAttrs = sku.shadow.attributes

    IlluminatedSku(id = sku.form.id,
                   code = model.code,
                   context = IlluminatedContext(context.name, context.attributes),
                   attributes = IlluminateAlgorithm.projectAttributes(formAttrs, shadowAttrs))
  }
}
