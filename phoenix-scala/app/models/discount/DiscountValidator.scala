package models.discount

import models.discount.DiscountHelpers._
import models.objects._
import services.discount.compilers.{OfferAstCompiler, QualifierAstCompiler}
import utils.IlluminateAlgorithm
import utils.aliases.EC
import utils.db._

/**
  * An DiscountValidator checks to make sure a discount shadow is valid
  */
object DiscountValidator {

  def validate(fs: FormAndShadow)(implicit ec: EC): DbResultT[Unit] =
    for {
      failures ← * <~ IlluminateAlgorithm.validateAttributes(fs.form.attributes,
                                                             fs.shadow.attributes)
      _ ← * <~ failIfFailures(failures)
      _ ← * <~ QualifierAstCompiler(fs.qualifier).compile()
      _ ← * <~ OfferAstCompiler(fs.offer).compile()
    } yield None
}
