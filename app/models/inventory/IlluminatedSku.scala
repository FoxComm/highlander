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

  def illuminate(productContext: ObjectContext, sku: Sku, form: ObjectForm, 
    shadow: ObjectShadow) : IlluminatedSku = { 

    IlluminatedSku(
      code = sku.code, 
      context = IlluminatedContext(productContext.name, productContext.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}

