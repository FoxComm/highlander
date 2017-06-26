package phoenix.models.shipping

import com.github.tminglei.slickpg.LTree
import shapeless._
import core.db.ExPostgresDriver.api._
import core.db._

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
    extends FoxTableQuery[DefaultShippingMethod, DefaultShippingMethods](new DefaultShippingMethods(_))
    with ReturningId[DefaultShippingMethod, DefaultShippingMethods] {

  val returningLens: Lens[DefaultShippingMethod, Int] = lens[DefaultShippingMethod].id

  private def resolveFirst[T, M](scope: LTree)(resolver: LTree ⇒ Query[T, M, Seq])(
      implicit ec: EC): DBIO[Option[M]] =
    resolver(scope).one
      .flatMap { // can blow up stack currently - please see https://github.com/slick/slick/pull/1703
        case None if scope.value.nonEmpty ⇒ resolveFirst(LTree(scope.value.init))(resolver)
        case other                        ⇒ DBIO.successful(other)
      }

  def findDefaultByScope(scope: LTree): QuerySeq = filter(_.scope === scope)

  def resolveDefault(scope: LTree)(implicit ec: EC): DBIO[Option[DefaultShippingMethod]] =
    resolveFirst(scope)(findDefaultByScope)

  def findByScope(scope: LTree): ShippingMethods.QuerySeq =
    findDefaultByScope(scope).join(ShippingMethods).on(_.shippingMethodId === _.id).map {
      case (_, sm) ⇒ sm
    }

  def resolve(scope: LTree)(implicit ec: EC): DBIO[Option[ShippingMethod]] =
    resolveFirst(scope)(findByScope)
}
