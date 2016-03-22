
package models.inventory

import failures.Failure
import org.json4s.jackson.Serialization.{write ⇒ render}
import utils.IlluminateAlgorithm

/**
 * An SkuValidator checks to make sure a sku shadow is valid
 */
object SkuValidator { 

  def validate(sku: Sku, shadow: SkuShadow) : Seq[Failure] = { 
    IlluminateAlgorithm.validateAttributes(sku.attributes, shadow.attributes) 
  }

}

