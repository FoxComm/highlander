package models.returns

import java.time.Instant

import com.pellucid.sealerate
import models.returns.Return._
import models.returns.ReturnReason._
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.ADT
import utils.db._

case class ReturnReason(id: Int = 0,
                        name: String,
                        reasonType: ReasonType = BaseReason,
                        rmaType: ReturnType = Standard,
                        createdAt: Instant = Instant.now,
                        deletedAt: Option[Instant] = None)
    extends FoxModel[ReturnReason] {}

object ReturnReason {
  sealed trait ReasonType
  case object BaseReason        extends ReasonType
  case object ProductReturnCode extends ReasonType

  object ReasonType extends ADT[ReasonType] {
    def types = sealerate.values[ReasonType]
  }

  implicit val reasonTypeColumnType: JdbcType[ReasonType] with BaseTypedType[ReasonType] =
    ReasonType.slickColumn
}

class ReturnReasons(tag: Tag) extends FoxTable[ReturnReason](tag, "return_reasons") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name       = column[String]("name")
  def reasonType = column[ReasonType]("reason_type")
  def returnType = column[ReturnType]("return_type")
  def createdAt  = column[Instant]("created_at")
  def deletedAt  = column[Option[Instant]]("deleted_at")

  def * =
    (id, name, reasonType, returnType, createdAt, deletedAt) <> ((ReturnReason.apply _).tupled, ReturnReason.unapply)
}

object ReturnReasons
    extends FoxTableQuery[ReturnReason, ReturnReasons](new ReturnReasons(_))
    with ReturningId[ReturnReason, ReturnReasons] {

  val returningLens: Lens[ReturnReason, Int] = lens[ReturnReason].id

  def findOneByReasonType(reasonType: ReasonType = BaseReason) = {}
}
