package phoenix.models.discount

import core.db._
import objectframework.IlluminateAlgorithm
import objectframework.models._
import org.json4s.Formats
import phoenix.models.discount.DiscountHelpers.{offer, qualifier}
import phoenix.services.discount.compilers.{OfferAstCompiler, QualifierAstCompiler}
import phoenix.utils.JsonFormatters

/**
  * An DiscountValidator checks to make sure a discount shadow is valid
  */
object DiscountValidator {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def validate(fs: FormAndShadow)(implicit ec: EC): DbResultT[Unit] =
    for {
      failures ← * <~ IlluminateAlgorithm.validateAttributes(fs.form.attributes, fs.shadow.attributes)
      _        ← * <~ failIfFailures(failures)
      _        ← * <~ QualifierAstCompiler(qualifier(fs.form, fs.shadow)).compile()
      _        ← * <~ OfferAstCompiler(offer(fs.form, fs.shadow)).compile()
    } yield None
}
