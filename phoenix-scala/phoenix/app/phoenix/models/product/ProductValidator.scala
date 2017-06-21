package phoenix.models.product

import core.failures.Failure
import objectframework.IlluminateAlgorithm
import objectframework.models._
import org.json4s.Formats
import phoenix.utils.JsonFormatters

/**
  * An ProductValidator checks to make sure a product shadow is valid
  */
object ProductValidator {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def validate(product: Product, form: ObjectForm, shadow: ObjectShadow): Seq[Failure] =
    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
}
