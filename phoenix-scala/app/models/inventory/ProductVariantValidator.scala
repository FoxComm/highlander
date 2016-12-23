package models.inventory

import failures.Failure
import models.objects._
import utils.IlluminateAlgorithm

/**
  * An ProductVariantValidator checks to make sure a variant shadow is valid
  */
object ProductVariantValidator {

  def validate(form: ObjectForm, shadow: ObjectShadow): Seq[Failure] = {
    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
  }
}
