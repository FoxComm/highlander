import phoenix.models.review.ProductReviews
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import phoenix.payloads.ProductReviewPayloads._
import phoenix.responses.ProductReviewResponses.ProductReviewResponse
import slick.jdbc.GetResult
import testutils.PayloadHelpers.tv
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtures
import core.db.ExPostgresDriver.api._

class ProductReviewIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with DefaultJwtAdminAuth
    with ApiFixtures
    with BakedFixtures
    with PhoenixAdminApi {

  "GET v1/review/:contextName/:reviewFormId" - {
    "gets product review" in new ProductReviewApiFixture {
      val getReviewResp = productReviewApi(productReview.id).get().as[ProductReviewResponse]
      getReviewResp must === (productReview)
    }
  }

  "POST v1/review/:contextName" - {
    "creates product review" in {
      val skuCode = ProductSku_ApiFixture().skuCode
      val payload =
        CreateProductReviewByCustomerPayload(attributes = "title" → tv("title"), sku = skuCode, scope = None)
      val reviewResp    = productReviewApi.create(payload).as[ProductReviewResponse]
      val getReviewResp = productReviewApi(reviewResp.id).get().as[ProductReviewResponse]
      getReviewResp must === (reviewResp)
      reviewResp.attributes must === (payload.attributes)
    }

    "does not create duplicate reviews" in {
      val skuCode = ProductSku_ApiFixture().skuCode
      val payload =
        CreateProductReviewByCustomerPayload(attributes = "title" → tv("title"), sku = skuCode, scope = None)
      val reviewResp1    = productReviewApi.create(payload).as[ProductReviewResponse]
      val getReviewResp1 = productReviewApi(reviewResp1.id).get().as[ProductReviewResponse]
      val reviewResp2    = productReviewApi.create(payload).as[ProductReviewResponse]
      val getReviewResp2 = productReviewApi(reviewResp2.id).get().as[ProductReviewResponse]
      getReviewResp1 must === (getReviewResp2)
    }
  }

  "PATCH v1/review/:contextName/:reviewFormId" - {
    "updates product review" in new ProductReviewApiFixture {
      val newAttributes = reviewAttributes ++ ("body" → tv("test"))
      val payload       = UpdateProductReviewPayload(newAttributes)
      val reviewResp    = productReviewApi(productReview.id).update(payload).as[ProductReviewResponse]
      reviewResp.attributes must === (payload.attributes)
    }
  }

  "DELETE v1/review/:contextName/:reviewFormId" - {
    "deletes product review" in new ProductReviewApiFixture {
      productReviewApi(productReview.id).delete.mustBeEmpty()
      val updatedReview = ProductReviews.mustFindById404(productReview.id).gimme
      updatedReview.archivedAt mustBe defined
    }
  }

  "product_reviews_search_view" - {

    case class ProductReviewsSearchViewItem(id: Int,
                                            scope: String,
                                            sku: String,
                                            userName: String,
                                            userId: Int,
                                            title: String,
                                            attributes: String,
                                            createdAt: Option[String],
                                            updatedAt: Option[String],
                                            archivedAt: Option[String])

    implicit val getProductReviewsSearchViewResult = GetResult(
      r ⇒
        ProductReviewsSearchViewItem(
          r.nextInt(),
          r.nextObject().toString,
          r.nextString(),
          r.nextString(),
          r.nextInt(),
          r.nextString(),
          r.nextString(),
          r.nextStringOption(),
          r.nextStringOption(),
          r.nextStringOption()
      ))

    def selectById(id: Int) =
      sql"""select * from product_reviews_search_view where id = $id"""
        .as[ProductReviewsSearchViewItem]
        .gimme

    "inserts new record on review insert" in {
      val title   = "title"
      val skuCode = ProductSku_ApiFixture().skuCode
      val payload =
        CreateProductReviewByCustomerPayload(attributes = "title" → tv(title), sku = skuCode, scope = None)
      val reviewResp = productReviewApi.create(payload).as[ProductReviewResponse]
      val value      = selectById(reviewResp.id).onlyElement
      value.id must === (reviewResp.id)
      value.title must === (title)
    }

    "updates record on review update" in new ProductReviewApiFixture {
      private val title: String = "newTitle"

      private val newTitle: JObject = "title" → tv(title)
      val newAttributes             = reviewAttributes.merge(newTitle)
      val payload                   = UpdateProductReviewPayload(newAttributes)
      val reviewResp                = productReviewApi(productReview.id).update(payload).as[ProductReviewResponse]

      val values = selectById(reviewResp.id)
      values.size must === (1)
      values(0).id must === (reviewResp.id)
      values(0).title must === (title)
    }

  }
}
