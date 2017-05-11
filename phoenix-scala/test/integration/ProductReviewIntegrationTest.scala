import akka.http.scaladsl.model.StatusCodes

import models.review.ProductReviews
import org.json4s._
import payloads.ProductReviewPayloads.{CreateProductReviewPayload, UpdateProductReviewPayload}
import responses.ProductReviewResponses.ProductReviewResponse
import testutils.PayloadHelpers.tv
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtures

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
    "creates product review" in new ProductSku_ApiFixture {
      val payload = CreateProductReviewPayload(attributes = Map("title" → tv("title")),
                                               sku = skuCode,
                                               scope = None)
      val reviewResp    = productReviewApi.create(payload).as[ProductReviewResponse]
      val getReviewResp = productReviewApi(reviewResp.id).get().as[ProductReviewResponse]
      getReviewResp must === (reviewResp)
      reviewResp.attributes must === (JObject(payload.attributes.toList: _*))
    }
  }

  "PATCH v1/review/:contextName/:reviewFormId" - {
    "updates product review" in new ProductReviewApiFixture {
      val newAttributes = reviewAttributes + ("testValue" → tv("test"))
      val payload       = UpdateProductReviewPayload(newAttributes)
      val reviewResp    = productReviewApi(productReview.id).update(payload).as[ProductReviewResponse]
      reviewResp.attributes must === (JObject(payload.attributes.toList: _*))
    }
  }

  "DELETE v1/review/:contextName/:reviewFormId" - {
    "deletes product review" in new ProductReviewApiFixture {
      val resp = productReviewApi(productReview.id).delete
      resp.status must === (StatusCodes.NoContent)
      val updatedReview = ProductReviews.mustFindByFormId404(productReview.id).gimme
      updatedReview.archivedAt mustBe defined
    }
  }
}
