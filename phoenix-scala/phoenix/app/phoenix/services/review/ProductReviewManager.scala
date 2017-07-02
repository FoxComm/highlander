package phoenix.services.review

import java.time.Instant

import phoenix.failures.ProductReviewFailures._
import phoenix.models.account.{Scope, User}
import phoenix.models.inventory.Skus
import phoenix.models.review.{ProductReview, ProductReviews}
import phoenix.payloads.ProductReviewPayloads._
import phoenix.responses.ProductReviewResponses
import phoenix.responses.ProductReviewResponses.ProductReviewResponse
import phoenix.utils.aliases._
import core.db._

object ProductReviewManager {

  def getReview(
      reviewId: ProductReview#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[ProductReviewResponse] =
    for {
      review ← * <~ ProductReviews.mustFindById404(reviewId)
      sku    ← * <~ Skus.mustFindById404(review.skuId)
    } yield ProductReviewResponses.build(review, sku.code)

  def createProductReview(userId: User#Id, payload: CreateProductReviewPayload)(
      implicit ec: EC,
      oc: OC,
      au: AU,
      db: DB): DbResultT[ProductReviewResponse] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      sku   ← * <~ Skus.mustFindByCode(payload.sku)
      productReview ← * <~ ProductReviews
                       .findOneByUserAndSku(userId, sku.id)
                       .findOrCreate(
                         ProductReviews.create(
                           ProductReview(scope = scope,
                                         content = payload.attributes,
                                         userId = userId,
                                         skuId = sku.id)))
      sku ← * <~ Skus.mustFindById404(productReview.skuId)
    } yield ProductReviewResponses.build(productReview, sku.code)

  def updateProductReview(reviewId: ProductReview#Id, payload: UpdateProductReviewPayload)(
      implicit ec: EC,
      oc: OC,
      db: DB,
      au: AU): DbResultT[ProductReviewResponse] =
    for {
      review ← * <~ ProductReviews.mustFindById404(reviewId)
      _      ← * <~ failIf(review.archivedAt.isDefined, ProductReviewIsArchived(reviewId))
      _      ← * <~ failIf(au.account.id != review.userId, ProductReviewUserMismatch(reviewId))
      newValue ← * <~ ProductReviews.update(
                  review,
                  review.copy(content = payload.attributes, updatedAt = Instant.now))
      sku ← * <~ Skus.mustFindById404(review.skuId)
    } yield ProductReviewResponses.build(newValue, sku.code)

  def archiveByContextAndId(reviewId: ProductReview#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[Unit] =
    for {
      review   ← * <~ ProductReviews.mustFindById404(reviewId)
      archived ← * <~ ProductReviews.update(review, review.copy(archivedAt = Some(Instant.now)))
    } yield ()

}
