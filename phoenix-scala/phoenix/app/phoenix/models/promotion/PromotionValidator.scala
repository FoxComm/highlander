package phoenix.models.promotion

import failures.Failure
import models.objects._
import org.json4s.Formats
import phoenix.utils.JsonFormatters
import utils.IlluminateAlgorithm

/**
  * An PromotionValidator checks to make sure a promotion shadow is valid
  */
object PromotionValidator {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def validate(promotion: Promotion, form: ObjectForm, shadow: ObjectShadow): Seq[Failure] = {

    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
  }
}
