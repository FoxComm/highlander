package models.discount

import models.Aliases.Json
import models.objects._

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant

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
final case class Discount(id: Int = 0, contextId: Int, shadowId: Int, formId: Int, 
  commitId: Int, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Discount]
  with Validation[Discount]

class Discounts(tag: Tag) extends ObjectHeads[Discount](tag, "discounts") {

  def * = (id, contextId, shadowId, formId, commitId, updatedAt, createdAt) <> ((Discount.apply _).tupled, Discount.unapply)

}

object Discounts extends TableQueryWithId[Discount, Discounts](
  idLens = GenLens[Discount](_.id))(new Discounts(_)) {

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)
}
