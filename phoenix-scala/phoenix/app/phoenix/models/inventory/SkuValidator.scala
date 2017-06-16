package phoenix.models.inventory

import core.failures.Failure
import objectframework.IlluminateAlgorithm
import objectframework.models._
import org.json4s.Formats
import phoenix.utils.JsonFormatters

/**
  * An SkuValidator checks to make sure a sku shadow is valid
  */
object SkuValidator {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def validate(form: ObjectForm, shadow: ObjectShadow): Seq[Failure] =
    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
}
