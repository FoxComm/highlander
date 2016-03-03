package models.product

import models.Aliases.Json
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JObject, JField, JNothing}

/**
 * An IlluminatedProduct is what you get when you combine the product shadow and
 * the product. 
 */
final case class IlluminatedProduct(productId: Int = 0, shadowId: Int = 0, 
  context: Json, attributes: Json, isActive: Boolean = true)

object IlluminatedProduct { 

  def illuminate(productContext: ProductContext, product: Product, 
    shadow: ProductShadow) : IlluminatedProduct = { 

    val context = productContext.context
    val attributes = projectAttributes(product.attributes, shadow.attributes)
    IlluminatedProduct(
      productId = product.id, 
      shadowId = shadow.id,
      context = context,
      attributes = attributes,
      isActive = product.isActive)
  }

  def findAttribute(attr: String, key: String, product: JObject) : JField = {
    product \ attr \ key match {
      case v ⇒  (attr, v)
    }
  }

  def projectAttributes(product: Json, shadow: Json) : Json = {
    shadow match {
      case JObject(s) ⇒  product match {
        case JObject(p) ⇒ 
          s.obj.map {
            case (attr, JString(key)) ⇒  findAttribute(attr, key, p)
            case (attr, _) ⇒  (attr, JNothing)
          }
        case _ ⇒ JNothing
      }
      case _ ⇒  JNothing
    }
  }
}

