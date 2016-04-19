package models.rma

import java.time.Instant

import cats.data.Validated._
import cats.data._
import com.pellucid.sealerate
import models.order.Order
import models.rma.Rma._
import models.traits.Lockable
import models.{StoreAdmin, javaTimeSlickMapper}
import monocle.Lens
import monocle.macros.GenLens
import failures.Failure
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.Validation._
import utils.table.SearchByRefNum
import utils.{ADT, FSM, GenericTable, ModelWithLockParameter, TableQueryWithLock}
import utils.aliases._

case class Rma(id: Int = 0, referenceNumber: String = "", orderId: Int, orderRefNum: String,
  rmaType: RmaType = Standard, state: State = Pending, isLocked: Boolean = false,
  customerId: Int, storeAdminId: Option[Int] = None, messageToCustomer: Option[String] = None,
  canceledReason: Option[Int] = None, createdAt: Instant = Instant.now,
  updatedAt: Instant = Instant.now, deletedAt: Option[Instant] = None)
  extends ModelWithLockParameter[Rma]
  with Lockable[Rma]
  with FSM[Rma.State, Rma] {

  def refNum: String = referenceNumber

  def stateLens = GenLens[Rma](_.state)
  override def primarySearchKeyLens: Lens[Rma, String] = GenLens[Rma](_.referenceNumber)

  val fsm: Map[State, Set[State]] = Map(
    Pending →
      Set(Processing, Canceled),
    Processing →
      Set(Review, Complete, Canceled),
    Review →
      Set(Complete, Canceled)
  )
}

object Rma {
  sealed trait State
  case object Pending extends State
  case object Processing extends State
  case object Review extends State
  case object Complete extends State
  case object Canceled extends State

  sealed trait RmaType
  case object Standard extends RmaType
  case object CreditOnly extends RmaType
  case object RestockOnly extends RmaType

  object RmaType extends ADT[RmaType] {
    def types = sealerate.values[RmaType]
  }

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val RmaTypeColumnType: JdbcType[RmaType] with BaseTypedType[RmaType] = RmaType.slickColumn
  implicit val StateTypeColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

  val rmaRefNumRegex = """([a-zA-Z0-9-_.]*)""".r
  val messageToCustomerMaxLength = 1000

  def build(order: Order, admin: StoreAdmin, rmaType: RmaType = Rma.Standard): Rma = {
    Rma(
      orderId = order.id,
      orderRefNum = order.refNum,
      rmaType = rmaType,
      customerId = order.customerId,
      storeAdminId = Some(admin.id)
    )
  }

  def validateStateReason(state: State, reason: Option[Int]): ValidatedNel[Failure, Unit] = {
    if (state == Canceled) {
      validExpr(reason.isDefined, "Please provide valid cancellation reason")
    } else {
      valid({})
    }
  }
}

class Rmas(tag: Tag) extends GenericTable.TableWithLock[Rma](tag, "rmas")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def orderId = column[Int]("order_id")
  def orderRefNum = column[String]("order_refnum")
  def rmaType = column[RmaType]("rma_type")
  def state = column[State]("state")
  def isLocked = column[Boolean]("is_locked")
  def customerId = column[Int]("customer_id")
  def storeAdminId = column[Option[Int]]("store_admin_id")
  def messageToCustomer = column[Option[String]]("message_to_customer")
  def canceledReason = column[Option[Int]]("canceled_reason")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * = (id, referenceNumber, orderId, orderRefNum, rmaType, state, isLocked, customerId, storeAdminId,
    messageToCustomer, canceledReason, createdAt, updatedAt, deletedAt) <> ((Rma.apply _).tupled, Rma.unapply)
}

object Rmas extends TableQueryWithLock[Rma, Rmas](
  idLens = GenLens[Rma](_.id)
)(new Rmas(_))
  with SearchByRefNum[Rma, Rmas] {

  val returningIdAndReferenceNumber = this.returning(map { rma ⇒ (rma.id, rma.referenceNumber) })

  def returningAction(ret: (Int, String))(rma: Rma): Rma = ret match {
    case (id, referenceNumber) ⇒ rma.copy(id = id, referenceNumber = referenceNumber)
  }

  override def create[R](rma: Rma, returning: Returning[R], action: R ⇒ Rma ⇒ Rma)
    (implicit ec: EC): DbResult[Rma] = super.create(rma, returningIdAndReferenceNumber, returningAction)

  def findByRefNum(refNum: String): QuerySeq = filter(_.referenceNumber === refNum)

  def findByCustomerId(customerId: Int): QuerySeq = filter(_.customerId === customerId)

  def findByOrderRefNum(refNum: String): QuerySeq = filter(_.orderRefNum === refNum)

  def findOneByRefNum(refNum: String): DBIO[Option[Rma]] = filter(_.referenceNumber === refNum).one

  def findOnePendingByRefNum(refNum: String): DBIO[Option[Rma]] =
    filter(_.referenceNumber === refNum).filter(_.state === (Rma.Pending: Rma.State)).one
}
