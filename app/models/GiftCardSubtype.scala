package models

import models.GiftCard.OriginType
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class GiftCardSubtype(id: Int = 0, title: String, originType: OriginType)
  extends ModelWithIdParameter

object GiftCardSubtype {
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] = OriginType.slickColumn
}

class GiftCardSubtypes(tag: Tag) extends GenericTable.TableWithId[GiftCardSubtype](tag, "gift_card_subtypes")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def originType = column[OriginType]("origin_type")

  def * = (id, title, originType) <> ((GiftCardSubtype.apply _).tupled, GiftCardSubtype.unapply)
}

object GiftCardSubtypes extends TableQueryWithId[GiftCardSubtype, GiftCardSubtypes](
  idLens = GenLens[GiftCardSubtype](_.id)
)(new GiftCardSubtypes(_)){
}
