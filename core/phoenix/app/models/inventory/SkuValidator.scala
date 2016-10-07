package models.inventory

import failures.Failure
import models.objects._
import utils.IlluminateAlgorithm

/**
  * An SkuValidator checks to make sure a sku shadow is valid
  */
object SkuValidator {

  def validate(form: ObjectForm, shadow: ObjectShadow): Seq[Failure] = {
    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
  }
}
