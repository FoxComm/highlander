
package models.product

import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import utils.IlluminateAlgorithm

import services.Failure
import services.ProductFailure._

import java.time.Instant
import monocle.macros.GenLens
import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}
import org.json4s.jackson.Serialization.{write ⇒ render}
import scala.concurrent.ExecutionContext

import models.Aliases.Json

/**
 * An ProductValidator checks to make sure a product shadow is valid
 */
object ProductValidator { 

  def validate( productContext: ProductContext, product: Product, 
    shadow: ProductShadow) : Seq[Failure] = { 

    IlluminateAlgorithm.validateAttributes(product.attributes, shadow.attributes) ++ 
    validateVariants(product.variants, productContext.name)
  }

  def validateVariants(variants: Json, context: String) : Seq[Failure] = {
    variants \ context match {
      case JNothing ⇒  Seq(NoVariantForContext(context))
      case v ⇒  Seq.empty
    }
  }
}

