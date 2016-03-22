
package models.product

import failures.Failure
import failures.ProductFailures.NoVariantForContext
import models.Aliases.Json
import org.json4s.JsonAST.JNothing
import org.json4s.jackson.Serialization.{write ⇒ render}
import utils.IlluminateAlgorithm

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

