import models.objects.ObjectContexts
import org.json4s.jackson.parseJson
import slick.profile.SqlStreamingAction
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api._
import utils.MockedApis
import utils.aliases.Json
import utils.db.ExPostgresDriver.api._

// TODO @anna move to separate test root
// TODO @anna extract query to shared package

case class ProductVariantSearchViewResult(id: Int,
                                          variant_id: Int,
                                          sku_code: String,
                                          context: String,
                                          context_id: Int,
                                          title: String,
                                          image: Option[String],
                                          sale_price: String,
                                          sale_price_currency: String,
                                          archived_at: Option[String],
                                          retail_price: String,
                                          retail_price_currency: String,
                                          external_id: Option[Int],
                                          scope: String,
                                          middlewarehouse_sku_id: Int)

class ProductVariantSearchViewTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with ApiFixtures
    with MockedApis {

  val query: SqlStreamingAction[Vector[String], String, Effect] =
    sql"select array_to_json(array_agg(sv)) from product_variants_search_view as sv".as[String]

  def searchViewResult: Seq[ProductVariantSearchViewResult] =
    parseJson(query.gimme.onlyElement).extract[Seq[ProductVariantSearchViewResult]]

  def priceAsString(attrs: Json, field: String): String =
    (attrs \ field \ "v" \ "value").extract[String]

  "Product variant search view" - {

    "must return result after insert" in new ProductVariant_ApiFixture {
      val variantSv = searchViewResult.onlyElement

      variantSv.variant_id must === (productVariant.id)
      variantSv.middlewarehouse_sku_id must === (productVariant.skuId)
      variantSv.archived_at must not be defined
      variantSv.retail_price must === (priceAsString(productVariant.attributes, "retailPrice"))
      variantSv.sale_price must === (priceAsString(productVariant.attributes, "salePrice"))
      variantSv.retail_price_currency must === ("USD")
      variantSv.sale_price_currency must === ("USD")
      variantSv.image must not be defined
      variantSv.sku_code must === (productVariantCode)
      variantSv.external_id must not be defined
      variantSv.title must === (productVariant.attributes.getString("title"))
      // scope?
      // context is not built for variant as part of product response; can call get
    }
  }
}
