package testutils

import models.inventory.ProductVariants
import models.objects.ObjectForm
import org.scalatest.AppendedClues
import utils.aliases._

trait IntegrationTestBase
    extends TestBase
    with DbTestSupport
    with GimmeSupport
    with AppendedClues {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def TEMPORARY_skuCodeToVariantFormId(skuCode: String)(implicit sl: SL, sf: SF): ObjectForm#Id =
    ProductVariants.findOneByCode(skuCode).gimme.value.formId withClue originalSourceClue

}
