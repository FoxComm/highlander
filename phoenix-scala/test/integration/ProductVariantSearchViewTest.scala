import models.objects.ObjectContexts
import org.json4s.jackson.parseJson
import payloads.ProductVariantPayloads.ProductVariantPayload
import responses.ProductVariantResponses.ProductVariantResponse
import slick.profile.SqlStreamingAction
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api._
import testutils.fixtures.api.products._
import utils.MockedApis
import utils.aliases.Json
import utils.db.ExPostgresDriver.api._

// TODO @anna move to separate test root
// TODO @anna extract query to shared package

case class ProductVariantSearchViewResult(id: Int,
                                          variantId: Int,
                                          skuCode: String,
                                          context: String,
                                          contextId: Int,
                                          title: String,
                                          image: Option[String],
                                          salePrice: String,
                                          salePriceCurrency: String,
                                          archivedAt: Option[String],
                                          retailPrice: String,
                                          retailPriceCurrency: String,
                                          externalId: Option[Int],
                                          scope: String,
                                          middlewarehouseSkuId: Int)

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
    parseJson(jsonString).camelizeKeys.extract[Seq[ProductVariantSearchViewResult]].onlyElement
  }

  def priceAsString(attrs: Json, field: String): String =
    (attrs \ field \ "v" \ "value").extract[String]

  "Product variant search view" - {

    "must return variant view after variant is created" in new ProductVariant_ApiFixture {
      val variantSv = findOneInSearchView(productVariant.id)

      {
        import variantSv._

        variantId must === (productVariant.id)
        middlewarehouseSkuId must === (productVariant.skuId)
        archivedAt must not be defined
        retailPrice must === (priceAsString(productVariant.attributes, "retailPrice"))
        salePrice must === (priceAsString(productVariant.attributes, "salePrice"))
        retailPriceCurrency must === ("USD")
        salePriceCurrency must === ("USD")
        image must not be defined
        skuCode must === (productVariantCode)
        externalId must not be defined
        title must === (productVariant.attributes.getString("title"))
        // scope?
        // context is not built for variant as part of product response; can call get
      }
    }

    "must update variant view when variant is updated" in new ProductVariant_ApiFixture {
      val newPrice = 123
      productVariantsApi(productVariant.id)
        .update(buildVariantPayload(code = productVariantCode, price = newPrice))
        .as[ProductVariantResponse.Root]

      val updated = findOneInSearchView(productVariant.id)
      updated.salePrice must === (newPrice.toString)
      updated.retailPrice must === (newPrice.toString)
    }
  }
}
