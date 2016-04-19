
package models.discount

import failures.Failure
import models.objects._
import utils.IlluminateAlgorithm

/**
 * An DiscountValidator checks to make sure a discount shadow is valid
 */
object DiscountValidator { 

  def validate(discount: Discount, form: ObjectForm, shadow: ObjectShadow) : Seq[Failure] = { 

    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
  }
}

