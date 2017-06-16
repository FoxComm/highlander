package phoenix.models.returns

import java.time.Instant

import com.pellucid.sealerate
import core.db._
import phoenix.models.returns.ReturnLineItem._
import phoenix.utils.ADT
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

case class ReturnLineItem(id: Int = 0,
                          returnId: Int,
                          reasonId: Int,
                          originType: OriginType,
                          createdAt: Instant = Instant.now)
    extends FoxModel[ReturnLineItem]

object ReturnLineItem {
  sealed trait OriginType  extends Product with Serializable
  case object SkuItem      extends OriginType
  case object ShippingCost extends OriginType

  implicit object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  implicit val OriginTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] =
    OriginType.slickColumn
}

class ReturnLineItems(tag: Tag) extends FoxTable[ReturnLineItem](tag, "return_line_items") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def returnId   = column[Int]("return_id")
  def reasonId   = column[Int]("reason_id")
  def originType = column[OriginType]("origin_type")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, returnId, reasonId, originType, createdAt) <> ((ReturnLineItem.apply _).tupled, ReturnLineItem.unapply)

  def returnReason = foreignKey(ReturnReasons.tableName, reasonId, ReturnReasons)(_.id)
}

object ReturnLineItems
    extends FoxTableQuery[ReturnLineItem, ReturnLineItems](new ReturnLineItems(_))
    with ReturningId[ReturnLineItem, ReturnLineItems] {
  val returningLens: Lens[ReturnLineItem, Int] = lens[ReturnLineItem].id

  def findByRmaId(returnId: Int): QuerySeq =
    filter(_.returnId === returnId)
}
