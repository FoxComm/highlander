package phoenix.services.review

import java.time.Instant

import core.db._
import phoenix.failures.ProductReviewFailures._
import phoenix.models.account.{Scope, User}
import phoenix.models.inventory.Skus
import phoenix.models.review.{ProductReview, ProductReviews}
import phoenix.payloads.ProductReviewPayloads._
import phoenix.responses.{ProductReviewResponse, ProductReviewsResponse}
import phoenix.utils.aliases._

object ProductReviewManager {

  def getReview(
      reviewId: ProductReview#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[ProductReviewResponse] =
    for {
      review ← * <~ ProductReviews.mustFindById404(reviewId)
      sku    ← * <~ Skus.mustFindById404(review.skuId)
    } yield ProductReviewResponse.build(review, sku.code)

  def getReviewForCustomer(
      userId: User#Id,
      reviewId: ProductReview#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[ProductReviewResponse] =
    for {
      review ← * <~ ProductReviews.mustFindById404(reviewId)
      sku    ← * <~ Skus.mustFindById404(review.skuId)
      _      ← * <~ failIf(userId != review.userId, FetchProductReviewUserMismatch(reviewId))
    } yield ProductReviewResponse.build(review, sku.code)

  def getAllReviewsForCustomer(
      userId: User#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[ProductReviewsResponse] =
    for {
      reviewsWithSkus ← * <~ ProductReviews.findAllWithSkusByUser(userId)
      reviewResponses = reviewsWithSkus.map {
        case (review, sku) ⇒ ProductReviewResponse.build(review, sku.code)
      }
    } yield ProductReviewsResponse(reviewResponses)

  def createProductReview(userId: User#Id, payload: CreateProductReviewPayload)(
      implicit ec: EC,
      oc: OC,
      au: AU,
      db: DB): DbResultT[ProductReviewResponse] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      sku   ← * <~ Skus.mustFindByCode(payload.sku)
      review ← * <~ ProductReviews
                .findOneByUserAndSku(userId, sku.id)
                .findOrCreate(
                  ProductReviews.create(
                    ProductReview(scope = scope,
                                  content = payload.attributes,
                                  userId = userId,
                                  skuId = sku.id)))
      sku ← * <~ Skus.mustFindById404(review.skuId)
    } yield ProductReviewResponse.build(review, sku.code)

  def updateProductReview(reviewId: ProductReview#Id, payload: UpdateProductReviewPayload)(
      implicit ec: EC,
      oc: OC,
      db: DB,
      au: AU): DbResultT[ProductReviewResponse] =
    for {
      review ← * <~ ProductReviews.mustFindById404(reviewId)
      _      ← * <~ failIf(review.archivedAt.isDefined, ProductReviewIsArchived(reviewId))
      _      ← * <~ failIf(au.account.id != review.userId, UpdateProductReviewUserMismatch(reviewId))
      newValue ← * <~ ProductReviews.update(
                  review,
                  review.copy(content = payload.attributes, updatedAt = Instant.now))
      sku ← * <~ Skus.mustFindById404(review.skuId)
    } yield ProductReviewResponse.build(newValue, sku.code)

  def archiveByContextAndId(reviewId: ProductReview#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[Unit] =
    for {
      review   ← * <~ ProductReviews.mustFindById404(reviewId)
      archived ← * <~ ProductReviews.update(review, review.copy(archivedAt = Some(Instant.now)))
    } yield {}

}
