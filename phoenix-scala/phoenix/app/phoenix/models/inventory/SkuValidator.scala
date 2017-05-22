package phoenix.models.inventory

import failures.Failure
import models.objects._
import org.json4s.Formats
import phoenix.utils.JsonFormatters
import utils.IlluminateAlgorithm

/**
  * An SkuValidator checks to make sure a sku shadow is valid
  */
object SkuValidator {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def validate(form: ObjectForm, shadow: ObjectShadow): Seq[Failure] = {
    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
  }
}