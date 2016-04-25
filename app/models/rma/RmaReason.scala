package models.rma

import java.time.Instant

import com.pellucid.sealerate
import models.rma.Rma._
import models.rma.RmaReason._
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.ADT
import utils.http.CustomDirectives.SortAndPage
import utils.aliases._
import utils.db._

case class RmaReason(id: Int = 0, name: String, reasonType: ReasonType = BaseReason, rmaType: RmaType = Standard,
  createdAt: Instant = Instant.now, deletedAt: Option[Instant] = None)
  extends FoxModel[RmaReason] {

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

class RmaReasons(tag: Tag) extends FoxTable[RmaReason](tag, "rma_reasons")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def reasonType = column[ReasonType]("reason_type")
  def rmaType = column[RmaType]("rma_type")
  def createdAt = column[Instant]("created_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * = (id, name, reasonType, rmaType, createdAt, deletedAt) <> ((RmaReason.apply _).tupled, RmaReason.unapply)
}

object RmaReasons extends FoxTableQuery[RmaReason, RmaReasons](
  idLens = GenLens[RmaReason](_.id)
)(new RmaReasons(_)) {

  def sortedAndPaged(query: QuerySeq)(implicit sortAndPage: SortAndPage): QuerySeqWithMetadata = {
    val sortedQuery = query.withMetadata.sortAndPageIfNeeded { case (s, reason) ⇒
      s.sortColumn match {
        case "id"           ⇒ if (s.asc) reason.id.asc           else reason.id.desc
        case "name"         ⇒ if (s.asc) reason.name.asc         else reason.name.desc
        case "reasonType"   ⇒ if (s.asc) reason.reasonType.asc   else reason.reasonType.desc
        case "rmaType"      ⇒ if (s.asc) reason.rmaType.asc      else reason.rmaType.desc
        case "createdAt"    ⇒ if (s.asc) reason.createdAt.asc    else reason.createdAt.desc
        case "deletedAt"    ⇒ if (s.asc) reason.deletedAt.asc    else reason.deletedAt.desc
        case other          ⇒ invalidSortColumn(other)
      }
    }

    sortedQuery.paged
  }

  def queryAll(implicit sortAndPage: SortAndPage): QuerySeqWithMetadata =
    this.sortedAndPaged(this)
}
