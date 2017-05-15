package services.review

import java.time.Instant

import failures.ProductReviewFailures.ProductReviewIsArchived
import models.account.Scope
import models.inventory.Skus
import models.objects._
import models.review.{ProductReview, ProductReviews}
import models.taxonomy.{TaxonLocation ⇒ _}
import payloads.ProductReviewPayloads._
import responses.ProductReviewResponses
import responses.ProductReviewResponses.ProductReviewResponse
import services.objects.ObjectManager
import utils.aliases._
import utils.db._

object ProductReviewManager {

  def getReview(reviewFormId: ObjectForm#Id)(implicit ec: EC,
                                             oc: OC,
                                             db: DB): DbResultT[ProductReviewResponse] =
    for {
      review ← * <~ ObjectManager.getFullObject(ProductReviews.mustFindByFormId404(reviewFormId))
      sku    ← * <~ Skus.mustFindById404(review.model.skuId)
    } yield ProductReviewResponses.build(review, sku.code)

  def createProductReview(userId: Int, payload: CreateProductReviewPayload)(
      implicit ec: EC,
      oc: OC,
      au: AU,
      db: DB): DbResultT[ProductReviewResponse] = {

    val form   = ObjectForm.fromPayload(ProductReview.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      ins   ← * <~ ObjectUtils.insert(form, shadow)
      sku   ← * <~ Skus.mustFindByCode(payload.sku)
      productReview ← * <~ ProductReviews.create(
                         ProductReview(scope = scope,
                                       contextId = oc.id,
                                       formId = ins.form.id,
                                       shadowId = ins.shadow.id,
                                       commitId = ins.commit.id,
                                       userId = userId,
                                       skuId = sku.id))
      sku ← * <~ Skus.mustFindById404(productReview.skuId)
    } yield ProductReviewResponses.build(FullObject(productReview, ins.form, ins.shadow), sku.code)
  }

  def updateProductReview(userId: Int,
                          reviewFormId: ObjectForm#Id,
                          payload: UpdateProductReviewPayload)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[ProductReviewResponse] = {

    val form   = ObjectForm.fromPayload(ProductReview.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      review ← * <~ ObjectManager.getFullObject(ProductReviews.mustFindByFormId404(reviewFormId))
      _      ← * <~ failIf(review.model.archivedAt.isDefined, ProductReviewIsArchived(reviewFormId))
      newValue ← * <~ ObjectUtils.commitUpdate(review,
                                               form.attributes,
                                               review.shadow.attributes.merge(shadow.attributes),
                                               ProductReviews.updateHead)
      sku ← * <~ Skus.mustFindById404(review.model.skuId)
    } yield ProductReviewResponses.build(newValue, sku.code)
  }

  def archiveByContextAndId(
      reviewFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[Unit] =
    for {
      review   ← * <~ ProductReviews.mustFindByFormId404(reviewFormId)
      archived ← * <~ ProductReviews.update(review, review.copy(archivedAt = Some(Instant.now)))
    } yield {}

}
