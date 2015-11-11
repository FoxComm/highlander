package models

import java.time.Instant

import scala.concurrent.ExecutionContext

import com.pellucid.sealerate
import models.Rma._
import monocle.Lens
import monocle.macros.GenLens

import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.CustomDirectives.SortAndPage

import utils.{FSM, ModelWithLockParameter, TableQueryWithLock, ADT, GenericTable}
import utils.Slick.implicits._
import utils.Slick.DbResult

final case class Rma(id: Int = 0, referenceNumber: String = "", orderId: Int, orderRefNum: String,
  rmaType: RmaType = Standard, status: Status = Pending, locked: Boolean = false,
  customerId: Int, storeAdminId: Option[Int] = None, createdAt: Instant = Instant.now,
  updatedAt: Instant = Instant.now, deletedAt: Option[Instant] = None)
  extends ModelWithLockParameter[Rma]
  with FSM[Rma.Status, Rma] {

  def refNum: String = referenceNumber

  def stateLens = GenLens[Rma](_.status)
  override def primarySearchKeyLens: Lens[Rma, String] = GenLens[Rma](_.referenceNumber)

  val fsm: Map[Status, Set[Status]] = Map(
    Pending →
      Set(Processing, Canceled),
    Processing →
      Set(Review, Complete, Canceled),
    Review →
      Set(Complete, Canceled)
  )
}

object Rma {
  sealed trait Status
  case object Pending extends Status
  case object Processing extends Status
  case object Review extends Status
  case object Complete extends Status
  case object Canceled extends Status

  sealed trait RmaType
  case object Standard extends RmaType
  case object CreditOnly extends RmaType
  case object RestockOnly extends RmaType

  object RmaType extends ADT[RmaType] {
    def types = sealerate.values[RmaType]
  }

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val RmaTypeColumnType: JdbcType[RmaType] with BaseTypedType[RmaType] = RmaType.slickColumn
  implicit val StatusTypeColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn

  val rmaRefNumRegex = """([a-zA-Z0-9-_.]*)""".r

  def build(order: Order, admin: StoreAdmin, rmaType: RmaType = Rma.Standard): Rma = {
    Rma(
      orderId = order.id,
      orderRefNum = order.refNum,
      rmaType = rmaType,
      customerId = order.customerId,
      storeAdminId = Some(admin.id)
    )
  }
}

class Rmas(tag: Tag) extends GenericTable.TableWithLock[Rma](tag, "rmas")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def orderId = column[Int]("order_id")
  def orderRefNum = column[String]("order_refnum")
  def rmaType = column[RmaType]("rma_type")
  def status = column[Status]("status")
  def locked = column[Boolean]("locked")
  def customerId = column[Int]("customer_id")
  def storeAdminId = column[Option[Int]]("store_admin_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * = (id, referenceNumber, orderId, orderRefNum, rmaType, status, locked, customerId,
    storeAdminId, createdAt, updatedAt, deletedAt) <> ((Rma.apply _).tupled, Rma.unapply)
}

object Rmas extends TableQueryWithLock[Rma, Rmas](
  idLens = GenLens[Rma](_.id)
)(new Rmas(_)) {

  override def primarySearchTerm: String = "referenceNumber"

  val returningIdAndReferenceNumber = this.returning(map { rma ⇒ (rma.id, rma.referenceNumber) })

  def returningAction(ret: (Int, String))(rma: Rma): Rma = ret match {
    case (id, referenceNumber) ⇒ rma.copy(id = id, referenceNumber = referenceNumber)
  }

  override def create[R](rma: Rma, returning: Returning[R], action: R ⇒ Rma ⇒ Rma)
    (implicit ec: ExecutionContext): DbResult[Rma] = super.create(rma, returningIdAndReferenceNumber, returningAction)

  def findByRefNum(refNum: String): QuerySeq = filter(_.referenceNumber === refNum)

  def findByCustomerId(customerId: Int): QuerySeq = filter(_.customerId === customerId)

  def findByOrderRefNum(refNum: String): QuerySeq = filter(_.orderRefNum === refNum)

  def findOneByRefNum(refNum: String): DBIO[Option[Rma]] = filter(_.referenceNumber === refNum).one

  def findOnePendingByRefNum(refNum: String): DBIO[Option[Rma]] =
    filter(_.referenceNumber === refNum).filter(_.status === (Rma.Pending: Rma.Status)).one
}
