package models.discount

import java.time.Instant

import models.objects._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.db._

import com.github.tminglei.slickpg._

object Discount {
  val kind = "discount"
}

/**
  * A Discount allows the customer to help sell a product by providing an incentive
  * such as price reductions. The discount is composed of three parts.
  *
  * 1. Merchandising
  * 2. Qualifier
  * 3. Offer
  *
  * The qualifier and offer are stored as attributes in the discount object and
  * are stored as a JSON representation of the AST for the Discount Algebra.
  *
  * The algebra is used to define the qualifier predicate and offer function.
  * used by the discount engine to modify an order by creating line item adjustments.
  */
case class Discount(id: Int = 0,
                    scope: LTree,
                    contextId: Int,
                    shadowId: Int,
                    formId: Int,
                    commitId: Int,
                    updatedAt: Instant = Instant.now,
                    createdAt: Instant = Instant.now,
                    archivedAt: Option[Instant] = None)
    extends FoxModel[Discount]
    with ObjectHead[Discount] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Discount =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class Discounts(tag: Tag) extends ObjectHeads[Discount](tag, "discounts") {

  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Discount.apply _).tupled,
    Discount.unapply)
}

object Discounts
    extends FoxTableQuery[Discount, Discounts](new Discounts(_))
    with ReturningId[Discount, Discounts] {

  val returningLens: Lens[Discount, Int] = lens[Discount].id

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)
}
