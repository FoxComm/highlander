package models

import com.pellucid.sealerate
import models.RmaReason.{ReasonType, BaseReason}
import models.Rma.{RmaType, Standard}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class RmaReason(id: Int = 0, name: String, reasonType: ReasonType = BaseReason, rmaType: RmaType = Standard)
  extends ModelWithIdParameter {

  def isNew: Boolean = id == 0
}

object RmaReason {
  sealed trait ReasonType
  case object BaseReason extends ReasonType
  case object ProductReturnCode extends ReasonType

  object ReasonType extends ADT[ReasonType] {
    def types = sealerate.values[ReasonType]
  }

  implicit val reasonTypeColumnType: JdbcType[ReasonType] with BaseTypedType[ReasonType] = ReasonType.slickColumn
}

class RmaReasons(tag: Tag) extends GenericTable.TableWithId[RmaReason](tag, "rma_reasons")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def reasonType = column[ReasonType]("reason_type")
  def rmaType = column[RmaType]("rma_type")

  def * = (id, name, reasonType, rmaType) <> ((RmaReason.apply _).tupled, RmaReason.unapply)
}

object RmaReasons extends TableQueryWithId[RmaReason, RmaReasons](
  idLens = GenLens[RmaReason](_.id)
)(new RmaReasons(_)) {
}
