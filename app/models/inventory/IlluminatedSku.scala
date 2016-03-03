package models.inventory

import utils.IlluminateAlgorithm
import models.product.{IlluminatedContext, ProductContext}
import models.Aliases.Json
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JObject, JField, JNothing}

/**
 * An IlluminatedSku is what you get when you combine the sku shadow and
 * the sku. 
 */
final case class IlluminatedSku(code: String, context: IlluminatedContext, attributes: Json)

object IlluminatedSku { 

  def illuminate(productContext: ProductContext, sku: Sku, shadow: SkuShadow) : IlluminatedSku = { 
    IlluminatedSku(
      code = sku.code, 
      context = IlluminatedContext(productContext.name, productContext.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(sku.attributes, shadow.attributes))
  }
}

