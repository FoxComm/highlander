package models.returns

import java.time.Instant

import cats.data.Validated._
import cats.data._
import com.pellucid.sealerate
import models.order.Order
import models.returns.Return._
import models.traits.Lockable
import models.StoreAdmin
import shapeless._
import failures.Failure
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Validation._
import utils.{ADT, FSM}
import utils.db._

case class Return(id: Int = 0,
                  referenceNumber: String = "",
                  orderId: Int,
                  orderRefNum: String,
                  returnType: ReturnType = Standard,
                  state: State = Pending,
                  isLocked: Boolean = false,
                  customerId: Int,
                  storeAdminId: Option[Int] = None,
                  messageToCustomer: Option[String] = None,
                  canceledReason: Option[Int] = None,
                  createdAt: Instant = Instant.now,
                  updatedAt: Instant = Instant.now,
                  deletedAt: Option[Instant] = None)
    extends FoxModel[Return]
    with Lockable[Return]
    with FSM[Return.State, Return] {

  def refNum: String = referenceNumber

  def stateLens                         = lens[Return].state
  override def primarySearchKey: String = referenceNumber

  val fsm: Map[State, Set[State]] = Map(
      Pending →
      Set(Processing, Canceled),
      Processing →
      Set(Review, Complete, Canceled),
      Review →
      Set(Complete, Canceled)
  )
}

object Return {
  sealed trait State
  case object Pending    extends State
  case object Processing extends State
  case object Review     extends State
  case object Complete   extends State
  case object Canceled   extends State

  sealed trait ReturnType
  case object Standard    extends ReturnType
  case object CreditOnly  extends ReturnType
  case object RestockOnly extends ReturnType

  object ReturnType extends ADT[ReturnType] {
    def types = sealerate.values[ReturnType]
  }

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val RmaTypeColumnType: JdbcType[ReturnType] with BaseTypedType[ReturnType] =
    ReturnType.slickColumn
  implicit val StateTypeColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

  val returnRefNumRegex          = """([a-zA-Z0-9-_.]*)""".r
  val messageToCustomerMaxLength = 1000

  def build(order: Order, admin: StoreAdmin, rmaType: ReturnType = Return.Standard): Return = {
    Return(
        orderId = order.id,
        orderRefNum = order.refNum,
        returnType = rmaType,
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

class Returns(tag: Tag) extends FoxTable[Return](tag, "returns") {
  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber   = column[String]("reference_number")
  def orderId           = column[Int]("order_id")
  def orderRefNum       = column[String]("order_refnum")
  def returnType        = column[ReturnType]("return_type")
  def state             = column[State]("state")
  def isLocked          = column[Boolean]("is_locked")
  def customerId        = column[Int]("customer_id")
  def storeAdminId      = column[Option[Int]]("store_admin_id")
  def messageToCustomer = column[Option[String]]("message_to_customer")
  def canceledReason    = column[Option[Int]]("canceled_reason")
  def createdAt         = column[Instant]("created_at")
  def updatedAt         = column[Instant]("updated_at")
  def deletedAt         = column[Option[Instant]]("deleted_at")

  def * =
    (id,
     referenceNumber,
     orderId,
     orderRefNum,
     returnType,
     state,
     isLocked,
     customerId,
     storeAdminId,
     messageToCustomer,
     canceledReason,
     createdAt,
     updatedAt,
     deletedAt) <> ((Return.apply _).tupled, Return.unapply)
}

object Returns
    extends FoxTableQuery[Return, Returns](new Returns(_))
    with ReturningIdAndString[Return, Returns]
    with SearchByRefNum[Return, Returns] {

  def findByRefNum(refNum: String): QuerySeq = filter(_.referenceNumber === refNum)

  def findByCustomerId(customerId: Int): QuerySeq = filter(_.customerId === customerId)

  def findByOrderRefNum(refNum: String): QuerySeq = filter(_.orderRefNum === refNum)

  def findOneByRefNum(refNum: String): DBIO[Option[Return]] =
    filter(_.referenceNumber === refNum).one

  def findOnePendingByRefNum(refNum: String): DBIO[Option[Return]] =
    filter(_.referenceNumber === refNum).filter(_.state === (Return.Pending: Return.State)).one

  private val rootLens                           = lens[Return]
  val returningLens: Lens[Return, (Int, String)] = rootLens.id ~ rootLens.referenceNumber
  override val returningQuery = map { rma ⇒
    (rma.id, rma.referenceNumber)
  }
}
