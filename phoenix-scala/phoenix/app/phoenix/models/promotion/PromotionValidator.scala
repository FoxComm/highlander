package phoenix.models.promotion

import core.failures.Failure
import objectframework.IlluminateAlgorithm
import objectframework.models._
import org.json4s.Formats
import phoenix.utils.JsonFormatters

/**
  * An PromotionValidator checks to make sure a promotion shadow is valid
  */
object PromotionValidator {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def validate(promotion: Promotion, form: ObjectForm, shadow: ObjectShadow): Seq[Failure] =
    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes)
}
