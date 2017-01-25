package models.payment.storecredit

import models.payment.storecredit.StoreCredit.OriginType
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class StoreCreditSubtype(id: Int = 0, title: String, originType: OriginType)
    extends FoxModel[StoreCreditSubtype]

class StoreCreditSubtypes(tag: Tag)
    extends FoxTable[StoreCreditSubtype](tag, "store_credit_subtypes") {

  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def title      = column[String]("title")
  def originType = column[OriginType]("origin_type")

  def * =
    (id, title, originType) <> ((StoreCreditSubtype.apply _).tupled, StoreCreditSubtype.unapply)
}

object StoreCreditSubtypes
    extends FoxTableQuery[StoreCreditSubtype, StoreCreditSubtypes](new StoreCreditSubtypes(_))
    with ReturningId[StoreCreditSubtype, StoreCreditSubtypes] {

  val returningLens: Lens[StoreCreditSubtype, Int] = lens[StoreCreditSubtype].id

  object scope {
    implicit class OriginTypeQuerySeqConversions(q: QuerySeq) {
      def giftCardTransfers: QuerySeq = q.byOriginType(StoreCredit.GiftCardTransfer)
      def csrAppeasements: QuerySeq   = q.byOriginType(StoreCredit.CsrAppeasement)
      def rmaProcesses: QuerySeq      = q.byOriginType(StoreCredit.RmaProcess)

      def byOriginType(originType: OriginType): QuerySeq =
        q.filter(_.originType === (originType: OriginType))
    }
  }
}
