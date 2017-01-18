package testutils

import models.inventory.ProductVariants
import models.objects.ObjectForm

trait IntegrationTestBase extends TestBase with DbTestSupport with GimmeSupport {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def TEMPORARY_skuCodeToVariantFormId(skuCode: String): ObjectForm#Id =
    ProductVariants.findOneByCode(skuCode).gimme.get.formId // safe to throw NSE here?

}
