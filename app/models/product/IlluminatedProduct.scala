
package models.product

import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant
import monocle.macros.GenLens
import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}
import org.json4s.jackson.Serialization.{write ⇒ render}
import scala.concurrent.ExecutionContext

import Aliases.Json

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

    val attributes = projectAttributes(product.attributes, shadow.attributes)
    IlluminatedProduct(
      productId = product.id, 
      shadowId = shadow.id,
      context = IlluminatedContext(name = productContext.name, attributes = productContext.attributes),
      attributes = attributes,
      variants = findVariants(product.variants, productContext.name))
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

  def findVariants(variants: Json, context: String) : JField = {
    variants \ context match {
      case v ⇒  (context, v)
    }
  }
}

