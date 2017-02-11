import java.time.Instant

import cats.implicits._
import models.objects.ObjectContexts
import payloads.ImagePayloads.{AlbumPayload, ImagePayload}
import responses.ProductVariantResponses.ProductVariantResponse
import testutils._
import testutils.fixtures.api.products._
import utils.Money.Currency
import utils.aliases.Json
import utils.db.ExPostgresDriver.api._

case class ProductVariantSearchViewResult(id: Int,
                                          variantId: Int,
                                          skuCode: String,
                                          // Why do we have both context name and context id in view?
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
                                          skuId: Int)

class ProductVariantSearchViewTest extends SearchViewTestBase {

  type SearchViewResult = ProductVariantSearchViewResult
  val searchViewName: String = "product_variants_search_view"
  val searchKeyName: String  = "variant_id"

  "Product variant search view must be updated" - {

    "when variant is created" in new ProductVariant_ApiFixture {
      val variantAsViewed = findOneInSearchView(productVariant.id)

      // Context is not built as part of variant response when embedded in product response ⇒ need to call GET
      val variantContextName = productVariantsApi(productVariant.id)
        .get()
        .as[ProductVariantResponse.Root]
        .context
        .value
        .name
      val variantContextId =
        ObjectContexts.filterByName(variantContextName).map(_.id).gimme.onlyElement

      {
        import variantAsViewed._

        variantId must === (productVariant.id)
        skuId must === (productVariant.skuId)
        archivedAt must not be defined
        retailPrice must === (productVariant.attributes.retailPrice.toString)
        salePrice must === (productVariant.attributes.salePrice.toString)
        retailPriceCurrency must === (Currency.USD.getCode)
        salePriceCurrency must === (Currency.USD.getCode)
        image must not be defined
        skuCode must === (productVariantCode)
        externalId must not be defined
        title must === (productVariant.attributes.getString("title"))
        context must === (variantContextName)
        contextId must === (variantContextId)
        // TODO how to check scope?
        scope must not be empty
      }
    }

    "when variant is updated" in new ProductVariant_ApiFixture {
      private val newPrice = 123
      private val imageSrc = "by the way we need image src validation..."
      private val albums = Seq(
          AlbumPayload(name = "Album McAlbumFace".some,
                       images = Seq(ImagePayload(src = imageSrc)).some)).some
      private val newTitle = "Variant McVariantFace"

      productVariantsApi(productVariant.id)
        .update(
            buildVariantPayload(code = productVariantCode,
                                price = newPrice,
                                albums = albums,
                                title = newTitle))
        .as[ProductVariantResponse.Root]

      private val updated = findOneInSearchView(productVariant.id)
      updated.salePrice must === (newPrice.toString)
      updated.retailPrice must === (newPrice.toString)
      updated.image.value must === (imageSrc)
      updated.title must === (newTitle)
    }

    "when variant is archived" in new ProductVariant_ApiFixture {
      productVariantsApi(productVariant.id).archive().mustBeOk()

      private val archivedAt =
        Instant.parse(findOneInSearchView(productVariant.id).archivedAt.value)
      archivedAt.isBefore(Instant.now) mustBe true
      archivedAt.isAfter(Instant.now.minusSeconds(1L)) mustBe true
    }
  }
}
