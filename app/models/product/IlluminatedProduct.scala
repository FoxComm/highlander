package models.product

import utils.IlluminateAlgorithm
import models.Aliases.Json
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JObject, JField, JNothing}
import java.time.Instant

final case class IlluminatedContext(name: String, attributes: Json)

/**
 * An IlluminatedProduct is what you get when you combine the product shadow and
 * the product. 
 */
final case class IlluminatedProduct(productId: Int = 0,
  context: IlluminatedContext, attributes: Json, variants: Json,
  activeFrom: Option[Instant], activeTo: Option[Instant])

object IlluminatedProduct { 

  def illuminate(productContext: ProductContext, product: Product, 
    shadow: ProductShadow) : IlluminatedProduct = { 
    IlluminatedProduct(
      productId = product.id, 
      context = IlluminatedContext(productContext.name, productContext.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(product.attributes, shadow.attributes),
      variants = findVariants(product.variants, shadow.variants),
      activeFrom = shadow.activeFrom, activeTo = shadow.activeTo)
  }

  def findVariants(variants: Json, key: String) : JField = {
    variants \ key match {
      case v ⇒  (key, v)
    }
  }
}

