package models

import models.StoreCredit.OriginType
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class StoreCreditSubtype(id: Int = 0, title: String, originType: OriginType)
  extends ModelWithIdParameter

object StoreCreditSubtype {
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] = OriginType.slickColumn
}

class StoreCreditSubtypes(tag: Tag) extends GenericTable.TableWithId[StoreCreditSubtype](tag, "gift_card_subtypes")  {
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
      def returnProcesses: QuerySeq  = q.byOriginType(StoreCredit.ReturnProcess)

      def byOriginType(originType: OriginType): QuerySeq = filter(_.originType === (originType: OriginType))
    }
  }
}
