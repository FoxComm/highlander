import models.inventory.{ProductVariantMwhSkuIds, ProductVariants}
import org.json4s.JsonDSL._
import org.scalacheck.Gen
import org.scalatest.EitherValues._
import org.scalatest.prop.PropertyChecks
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductVariantPayloads.ProductVariantPayload
import responses.ProductVariantResponses.ProductVariantResponse
import scala.util.Random
import server.SKUsMigration
import slick.driver.PostgresDriver.api._
import testutils.apis.PhoenixAdminApi
import testutils._
import utils.MockedApis
import utils.aliases._
import utils.db._

class SKUsMigrationIntegrationTest
    extends IntegrationTestBase
    with AutomaticAuth
    with MockedApis
    with PhoenixAdminApi
    with PropertyChecks {
  def makeSkuPayload(code: String,
                     attrMap: Map[String, Json],
                     albums: Option[Seq[AlbumPayload]] = None) = {
    val codeJson   = ("t"              → "string") ~ ("v" → code)
    val attributes = attrMap + ("code" → codeJson)
    ProductVariantPayload(attributes = attributes, albums = albums)
  }

  "SKUsMigration should send request to MWH for all product variants that don't have sku id created" in {
    implicit val au: AU =
      AuthAs(authedUser, authedCustomer).checkAuthUser(None).futureValue.right.value
    val migration  = new SKUsMigration(middlewarehouseApiMock)
    val payloads   = (1 to 100).map(_ ⇒ makeSkuPayload(Random.nextString(10), Map.empty, None)).toSet
    val variantIds = payloads.map(skusApi.create(_).as[ProductVariantResponse.Root].id)

    val validations = for {
      // insert product variants
      pvs ← * <~ ProductVariants.findAllByIds(variantIds).result
      formIds = pvs.map(_.formId).toSet
      // we need this to replicate state before mvh sku ids table was created
      deleted ← * <~ ProductVariantMwhSkuIds.filter(_.variantFormId inSet formIds).delete
      // run actual migration
      _          ← * <~ migration.run()
      toValidate ← * <~ formIds.map(ProductVariantMwhSkuIds.mustFindMwhSkuId)
      // clean
      _ ← * <~ ProductVariants.filter(_.id inSet variantIds).delete
    } yield {
      deleted must === (formIds.size)
      toValidate.foreach(id ⇒ formIds must contain(id))
    }

    validations.run().futureValue.fold(errs ⇒ sys.error(errs.flatten.mkString("\n")), _ ⇒ ())
  }
}
