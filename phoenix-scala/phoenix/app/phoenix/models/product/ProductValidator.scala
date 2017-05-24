package phoenix.models.product

import failures.Failure
import models.objects._
import org.json4s.Formats
import phoenix.utils.JsonFormatters
import utils.IlluminateAlgorithm

/**
  * An ProductValidator checks to make sure a product shadow is valid
  */
object ProductValidator {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def validate(product: Product, form: ObjectForm, shadow: ObjectShadow): Seq[Failure] = {

    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
  }
}