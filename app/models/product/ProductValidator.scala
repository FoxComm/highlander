
package models.product

import failures.Failure
import models.objects._
import org.json4s.jackson.Serialization.{write ⇒ render}
import utils.IlluminateAlgorithm

/**
 * An ProductValidator checks to make sure a product shadow is valid
 */
object ProductValidator { 

  def validate(product: Product, form: ObjectForm, shadow: ObjectShadow) : Seq[Failure] = { 

    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
  }
}

