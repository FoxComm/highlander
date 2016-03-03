package models.product

import utils.IlluminateAlgorithm
import models.Aliases.Json
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JObject, JField, JNothing}

final case class IlluminatedContext(name: String, attributes: Json)

/**
 * An IlluminatedProduct is what you get when you combine the product shadow and
 * the product. 
 */
final case class IlluminatedProduct(productId: Int = 0, shadowId: Int = 0, 
  context: IlluminatedContext, attributes: Json, variants: Json)

object IlluminatedProduct { 

  def illuminate(productContext: ProductContext, product: Product, 
    shadow: ProductShadow) : IlluminatedProduct = { 
    IlluminatedProduct(
      productId = product.id, 
      shadowId = shadow.id,
      context = IlluminatedContext(productContext.name, productContext.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(product.attributes, shadow.attributes),
      variants = findVariants(product.variants, productContext.name))
  }

  def findVariants(variants: Json, context: String) : JField = {
    variants \ context match {
      case v ⇒  (context, v)
    }
  }
}

