package models.review

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import models.objects._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.Validation
import utils.db._

object ProductReview {
  val kind = "product-review"
}

case class ProductReview(id: Int = 0,
                         scope: LTree,
                         contextId: Int,
                         shadowId: Int,
                         formId: Int,
                         commitId: Int,
                         userId: Int,
                         skuId: Int,
                         updatedAt: Instant = Instant.now,
                         createdAt: Instant = Instant.now,
                         archivedAt: Option[Instant] = None)
    extends FoxModel[ProductReview]
    with Validation[ProductReview]
    with ObjectHead[ProductReview] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): ProductReview =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class ProductReviews(tag: Tag) extends ObjectHeads[ProductReview](tag, "product_reviews") {

  def userId = column[Int]("user_id")
  def skuId  = column[Int]("sku_id")
  def * =
    (id,
     scope,
     contextId,
     shadowId,
     formId,
     commitId,
     userId,
     skuId,
     updatedAt,
     createdAt,
     archivedAt) <> ((ProductReview.apply _).tupled, ProductReview.unapply)
}

object ProductReviews
    extends ObjectHeadsQueries[ProductReview, ProductReviews](new ProductReviews(_))
    with ReturningId[ProductReview, ProductReviews] {

  val returningLens: Lens[ProductReview, Int] = lens[ProductReview].id
}
