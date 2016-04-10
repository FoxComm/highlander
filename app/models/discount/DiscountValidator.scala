
package models.discount

import failures.Failure
import models.objects._
import models.Aliases.Json
import org.json4s.JsonAST.JNothing
import org.json4s.jackson.Serialization.{write ⇒ render}
import utils.IlluminateAlgorithm

/**
 * An DiscountValidator checks to make sure a discount shadow is valid
 */
object DiscountValidator { 

  def validate(discount: Discount, form: ObjectForm, shadow: ObjectShadow) : Seq[Failure] = { 

    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
  }
}

