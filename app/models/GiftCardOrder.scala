package models

import com.pellucid.sealerate
import services.Failure
import slick.dbio
import slick.dbio.Effect.Write
import utils.Money._
import utils.{ADT, GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}
import validators.nonEmptyIf

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class GiftCardOrder(id: Int = 0, orderId: Int) extends ModelWithIdParameter

object GiftCardOrder {}

class GiftCardOrders(tag: Tag) extends GenericTable.TableWithId[GiftCardOrder](tag, "gift_card_orders") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")

  def * = (id, orderId) <> ((GiftCardOrder.apply _).tupled, GiftCardOrder.unapply)
}

object GiftCardOrders extends TableQueryWithId[GiftCardOrder, GiftCardOrders](
  idLens = GenLens[GiftCardOrder](_.id)
  )(new GiftCardOrders(_)){
}
