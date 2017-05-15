package models.discount

import models.discount.DiscountHelpers.{offer, qualifier}
import models.objects._
import org.json4s.Formats
import services.discount.compilers.{OfferAstCompiler, QualifierAstCompiler}
import utils.{IlluminateAlgorithm, JsonFormatters}
import utils.aliases.EC
import utils.db._

/**
  * An DiscountValidator checks to make sure a discount shadow is valid
  */
object DiscountValidator {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def validate(fs: FormAndShadow)(implicit ec: EC): DbResultT[Unit] =
    for {
      failures ← * <~ IlluminateAlgorithm.validateAttributes(fs.form.attributes,
                                                             fs.shadow.attributes)
      _ ← * <~ failIfFailures(failures)
      _ ← * <~ QualifierAstCompiler(qualifier(fs.form, fs.shadow)).compile()
      _ ← * <~ OfferAstCompiler(offer(fs.form, fs.shadow)).compile()
    } yield None
}
