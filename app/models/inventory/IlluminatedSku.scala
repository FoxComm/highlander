package models.inventory

import models.Aliases.Json
import models.objects._
import utils.IlluminateAlgorithm

/**
 * An IlluminatedSku is what you get when you combine the sku shadow and
 * the sku. 
 */
case class IlluminatedSku(code: String, context: IlluminatedContext, 
  attributes: Json)

object IlluminatedSku { 

  def illuminate(context: ObjectContext, sku: FullObject[Sku]) : IlluminatedSku = {
    val model = sku.model
    val formAttrs = sku.form.attributes
    val shadowAttrs = sku.shadow.attributes

    IlluminatedSku(
      code = model.code,
      context = IlluminatedContext(context.name, context.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(formAttrs, shadowAttrs))
  }
}

