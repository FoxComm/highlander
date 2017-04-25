package models.promotion

import failures.Failure
import models.objects._
import utils.IlluminateAlgorithm

/**
  * An PromotionValidator checks to make sure a promotion shadow is valid
  */
object PromotionValidator {

  def validate(promotion: Promotion, form: ObjectForm, shadow: ObjectShadow): Seq[Failure] = {

    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
  }
}
