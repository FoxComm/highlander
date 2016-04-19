package models.payment.storecredit

import models.payment.storecredit.StoreCredit.OriginType
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

case class StoreCreditSubtype(id: Int = 0, title: String, originType: OriginType)
  extends ModelWithIdParameter[StoreCreditSubtype]

object StoreCreditSubtype {}

class StoreCreditSubtypes(tag: Tag)
  extends GenericTable.TableWithId[StoreCreditSubtype](tag, "store_credit_subtypes")  {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def originType = column[OriginType]("origin_type")

  def * = (id, title, originType) <> ((StoreCreditSubtype.apply _).tupled, StoreCreditSubtype.unapply)
}

object StoreCreditSubtypes extends TableQueryWithId[StoreCreditSubtype, StoreCreditSubtypes](
  idLens = GenLens[StoreCreditSubtype](_.id)
)(new StoreCreditSubtypes(_)){

  object scope {
    implicit class OriginTypeQuerySeqConversions(q: QuerySeq) {
      def giftCardTransfers: QuerySeq = q.byOriginType(StoreCredit.GiftCardTransfer)
      def csrAppeasements: QuerySeq  = q.byOriginType(StoreCredit.CsrAppeasement)
      def rmaProcesses: QuerySeq  = q.byOriginType(StoreCredit.RmaProcess)

      def byOriginType(originType: OriginType): QuerySeq = q.filter(_.originType === (originType: OriginType))
    }
  }
}
