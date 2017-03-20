package models.shipping

import com.github.tminglei.slickpg.LTree
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class DefaultShippingMethod(id: Int = 0, scope: LTree, shippingMethodId: ShippingMethod#Id)
    extends FoxModel[DefaultShippingMethod]

class DefaultShippingMethods(tag: Tag)
    extends FoxTable[DefaultShippingMethod](tag, "default_shipping_methods") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope            = column[LTree]("scope")
  def shippingMethodId = column[Int]("shipping_method_id")

  def * =
    (id, scope, shippingMethodId) <> ((DefaultShippingMethod.apply _).tupled, DefaultShippingMethod.unapply)
}

object DefaultShippingMethods
    extends FoxTableQuery[DefaultShippingMethod, DefaultShippingMethods](
        new DefaultShippingMethods(_))
    with ReturningId[DefaultShippingMethod, DefaultShippingMethods] {

  val returningLens: Lens[DefaultShippingMethod, Int] = lens[DefaultShippingMethod].id

  def findDefaultByScope(scope: LTree): QuerySeq = filter(_.scope === scope)

  def findByScope(scope: LTree): ShippingMethods.QuerySeq =
    findDefaultByScope(scope).join(ShippingMethods).on(_.shippingMethodId === _.id).map {
      case (_, sm) ⇒ sm
    }
}
