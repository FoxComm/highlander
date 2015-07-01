package models

import utils.{GenericTable, TableQueryWithId, ModelWithIdParameter, RichTable}

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

case class GiftCardBalance(id: Long = 0, giftCardId: Int, credit: Int, debit: Int)
  extends ModelWithIdParameter {

  override type Id = Long
}

object GiftCardBalance {}


class GiftCardBalances(tag: Tag) extends GenericTable.TableWithId[GiftCardBalance](tag, "gift_card_balances") with RichTable {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")
  def credit = column[Int]("credit")
  def debit = column[Int]("debit")

  def * = (id, giftCardId, credit, debit) <> ((GiftCardBalance.apply _).tupled, GiftCardBalance.unapply)
}

object GiftCardBalances extends TableQueryWithId[GiftCardBalance, GiftCardBalances](
  idLens = GenLens[GiftCardBalance](_.id)
  )(new GiftCardBalances(_)){
}
