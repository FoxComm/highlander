package models

import scala.concurrent.{ExecutionContext, Future}

import com.pellucid.sealerate
import com.wix.accord.dsl.{validator ⇒ createValidator}
import com.wix.accord.{Failure ⇒ ValidationFailure}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils._
import OrderLineItem.{Status, Cart}

final case class OrderLineItem(id: Int = 0, orderId: Int, skuId: Int,
  status: Status = Cart)
  extends ModelWithIdParameter
  with FSM[OrderLineItem.Status, OrderLineItem] {

  import OrderLineItem._

  def stateLens = GenLens[OrderLineItem](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    Cart →
      Set(Pending, PreOrdered, BackOrdered, Canceled),
    Pending →
      Set(Shipped, Canceled),
    PreOrdered →
      Set(Shipped, Canceled),
    BackOrdered →
      Set(Shipped, Canceled)
  )
}

object OrderLineItem{
  sealed trait Status
  case object Cart extends Status
  case object Pending extends Status
  case object PreOrdered extends Status
  case object BackOrdered extends Status
  case object Canceled extends Status
  case object Shipped extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType = Status.slickColumn
}

class OrderLineItems(tag: Tag) extends GenericTable.TableWithId[OrderLineItem](tag, "order_line_items") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def skuId = column[Int]("sku_id")
  def status = column[OrderLineItem.Status]("status")
  def * = (id, orderId, skuId, status) <> ((OrderLineItem.apply _).tupled, OrderLineItem.unapply)
}

object OrderLineItems extends TableQueryWithId[OrderLineItem, OrderLineItems](
  idLens = GenLens[OrderLineItem](_.id)
)(new OrderLineItems(_)) {

  def findByOrder(order: Order)(implicit db: Database) = db.run(_findByOrderId(order.id).result)

  def _findByOrder(order: Order): Query[OrderLineItems, OrderLineItem, Seq] =
    _findByOrderId(order.id)

  def _findByOrderId(orderId: Rep[Int]) = { filter(_.orderId === orderId) }

  def countByOrder(order: Order)(implicit ec: ExecutionContext, db: Database) =
    db.run(this._countByOrder(order))

  def _countByOrder(order: Order) =
    _findByOrderId(order.id).length.result

  def countBySkuIdForOrder(order: Order)(implicit ec: ExecutionContext, db: Database): Future[Seq[(Int, Int)]] =
    db.run(_countBySkuIdForOrder(order))

  def _countBySkuIdForOrder(order: Order) =
    (for {
      (skuId, group) <- _findByOrderId(order.id).groupBy(_.skuId)
    } yield (skuId, group.length)).result
}
