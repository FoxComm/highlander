import models.inventory.{ProductVariantMwhSkuIds, ProductVariants}
import org.scalatest.prop.PropertyChecks
import responses.ProductVariantResponses.ProductVariantResponse
import scala.util.Random
import server.SkusMigration
import slick.driver.PostgresDriver.api._
import testutils.apis.PhoenixAdminApi
import testutils._
import utils.MockedApis
import utils.aliases._
import utils.db._

class SkusMigrationIntegrationTest
    extends IntegrationTestBase
    with AutomaticAuth
    with GimmeSupport
    with ProductVariantHelpers
    with MockedApis
    with PhoenixAdminApi
    with PropertyChecks {

  "SKUsMigration should send request to MWH for all product variants that don't have sku id created" in {
    implicit val au: AU = AuthAs(authedUser, authedCustomer).checkAuthUser(None).gimme
    val payloads        = (1 to 100).map(_ ⇒ makeSkuPayload(Random.nextString(10), Map.empty, None)).toSet
    val variantIds      = payloads.map(productVariantsApi.create(_).as[ProductVariantResponse.Root].id)

    val validations = for {
      // insert product variants
      pvs ← * <~ ProductVariants.findAllByIds(variantIds).result
      formIds = pvs.map(_.formId).toSet
      // we need this to replicate state before mvh sku ids table was created
      deleted ← * <~ ProductVariantMwhSkuIds.filter(_.variantFormId inSet formIds).delete
      // run actual migration
      _          ← * <~ SkusMigration.migrate(10)
      toValidate ← * <~ formIds.map(ProductVariantMwhSkuIds.mustFindMwhSkuId)
      // clean
      _ ← * <~ ProductVariants.filter(_.id inSet variantIds).delete
    } yield {
      deleted must === (formIds.size)
      toValidate.foreach(id ⇒ formIds must contain(id))
    }

    validations.gimme
  }
}
