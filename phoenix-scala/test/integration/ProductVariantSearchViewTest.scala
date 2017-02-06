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

  val searchViewName: String = "product_variants_search_view"

  def findOneInSearchView(id: Int): ProductVariantSearchViewResult = {
    val query =
      sql"select array_to_json(array_agg(sv)) from #$searchViewName as sv where sv.variant_id=#$id"
        .as[String]
    val jsonString = query.gimme.onlyElement
    withClue("Query result was empty. Slick returns Vector(null) instead of empty Vector.\n") {
      jsonString must not be null
    }
    parseJson(jsonString).extract[Seq[ProductVariantSearchViewResult]].onlyElement
  }

  def priceAsString(attrs: Json, field: String): String =
    (attrs \ field \ "v" \ "value").extract[String]

  "Product variant search view" - {

    "must return result after insert" in new ProductVariant_ApiFixture {
      val variantSv = findOneInSearchView(productVariant.id)

      {
        import variantSv._

        variant_id must === (productVariant.id)
        middlewarehouse_sku_id must === (productVariant.skuId)
        archived_at must not be defined
        retail_price must === (priceAsString(productVariant.attributes, "retailPrice"))
        sale_price must === (priceAsString(productVariant.attributes, "salePrice"))
        retail_price_currency must === ("USD")
        sale_price_currency must === ("USD")
        image must not be defined
        sku_code must === (productVariantCode)
        external_id must not be defined
        title must === (productVariant.attributes.getString("title"))
        // scope?
        // context is not built for variant as part of product response; can call get
      }
    }
  }
}
