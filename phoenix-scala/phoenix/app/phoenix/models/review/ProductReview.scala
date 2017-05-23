package phoenix.models.review

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import phoenix.models.objects._
import shapeless._
import slick.lifted.Tag
import utils.Validation
import phoenix.utils.aliases.Json
import utils.db._
import utils.db.ExPostgresDriver.api._

case class ProductReview(id: Int = 0,
                         scope: LTree,
                         content: Json,
                         userId: Int,
                         skuId: Int,
                         updatedAt: Instant = Instant.now,
                         createdAt: Instant = Instant.now,
                         archivedAt: Option[Instant] = None)
    extends FoxModel[ProductReview]
    with Validation[ProductReview]

class ProductReviews(tag: Tag) extends FoxTable[ProductReview](tag, "product_reviews") {

  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope      = column[LTree]("scope")
  def content    = column[Json]("content")
  def userId     = column[Int]("user_id")
  def skuId      = column[Int]("sku_id")
  def updatedAt  = column[Instant]("updated_at")
  def createdAt  = column[Instant]("created_at")
  def archivedAt = column[Option[Instant]]("archived_at")

  def * =
    (id, scope, content, userId, skuId, updatedAt, createdAt, archivedAt) <> ((ProductReview.apply _).tupled, ProductReview.unapply)
}

object ProductReviews
    extends FoxTableQuery[ProductReview, ProductReviews](new ProductReviews(_))
    with ReturningId[ProductReview, ProductReviews] {

  val returningLens: Lens[ProductReview, Int] = lens[ProductReview].id
}
