package models.returns

import cats.data.{State ⇒ _}
import com.pellucid.sealerate
import failures.NotFoundFailure404
import java.time.Instant
import models.account._
import models.cord.{Order, Orders}
import models.returns.Return._
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.aliases._
import utils.db._
import utils.{ADT, FSM}

case class Return(id: Int = 0,
                  referenceNumber: String = "",
                  orderId: Int,
                  orderRef: String,
                  returnType: ReturnType = Standard,
                  state: State = Pending,
                  accountId: Int,
                  storeAdminId: Option[Int] = None,
                  messageToAccount: Option[String] = None,
                  canceledReasonId: Option[Int] = None,
                  createdAt: Instant = Instant.now,
                  updatedAt: Instant = Instant.now,
                  deletedAt: Option[Instant] = None)
    extends FoxModel[Return]
    with FSM[Return.State, Return] {

  def refNum: String = referenceNumber

  def stateLens = lens[Return].state

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

  val returnRefNumRegex         = """([a-zA-Z0-9-_.]*)""".r // normally it's "[Order.refNumber].#"
  val messageToAccountMaxLength = 1000

  def build(order: Order, admin: User, rmaType: ReturnType = Return.Standard): Return = {
    Return(
        orderId = order.id,
        orderRef = order.refNum,
        returnType = rmaType,
        accountId = order.accountId,
        storeAdminId = Some(admin.accountId)
    )
  }

}

class Returns(tag: Tag) extends FoxTable[Return](tag, "returns") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber  = column[String]("reference_number")
  def orderId          = column[Int]("order_id")
  def orderRef         = column[String]("order_ref")
  def returnType       = column[ReturnType]("return_type")
  def state            = column[State]("state")
  def accountId        = column[Int]("account_id")
  def storeAdminId     = column[Option[Int]]("store_admin_id")
  def messageToAccount = column[Option[String]]("message_to_account")
  def canceledReasonId = column[Option[Int]]("canceled_reason_id") // see models.Reasons
  def createdAt        = column[Instant]("created_at")
  def updatedAt        = column[Instant]("updated_at")
  def deletedAt        = column[Option[Instant]]("deleted_at")

  def * =
    (id,
     referenceNumber,
     orderId,
     orderRef,
     returnType,
     state,
     accountId,
     storeAdminId,
     messageToAccount,
     canceledReasonId,
     createdAt,
     updatedAt,
     deletedAt) <> ((Return.apply _).tupled, Return.unapply)

  // TODO why have both reference to order id and order ref ?
  def orderIdFKey =
    foreignKey("returns_order_id_fkey", orderId, Orders)(_.id,
                                                         onUpdate = ForeignKeyAction.Restrict,
                                                         onDelete = ForeignKeyAction.Restrict)
  def orderRefFKey =
    foreignKey("returns_order_ref_fkey", orderRef, Orders)(_.referenceNumber,
                                                           onUpdate = ForeignKeyAction.Restrict,
                                                           onDelete = ForeignKeyAction.Restrict)
}

object Returns
    extends FoxTableQuery[Return, Returns](new Returns(_))
    with ReturningIdAndString[Return, Returns]
    with SearchByRefNum[Return, Returns] {
  private[this] val activeStates = Set(Return.Pending, Return.Processing, Return.Review)

  def findByRefNum(refNum: String): QuerySeq = filter(_.referenceNumber === refNum)

  def findByAccountId(accountId: Int): QuerySeq = filter(_.accountId === accountId)

  def findByOrderRefNum(refNum: String): QuerySeq = filter(_.orderRef === refNum)

  def findPrevious(rma: Return): QuerySeq =
    findByOrderRefNum(rma.orderRef).filter(r ⇒
          r.id =!= rma.id && r.state === (Return.Complete: Return.State))

  def findPreviousOrCurrent(rma: Return): QuerySeq =
    findByOrderRefNum(rma.orderRef).filter(r ⇒
          (r.id =!= rma.id && r.state === (Return.Complete: Return.State)) || r.id === rma.id)

  def findOneByRefNum(refNum: String): DBIO[Option[Return]] =
    findByRefNum(refNum).one

  def mustFindActiveByRefNum404(refNum: String)(implicit ec: EC): DbResultT[Return] =
    findByRefNum(refNum)
      .filter(_.state inSet activeStates)
      .mustFindOneOr(NotFoundFailure404(Return, refNum))

  private[this] val rootLens = lens[Return]

  val returningLens: Lens[Return, (Int, String)] = rootLens.id ~ rootLens.referenceNumber

  override val returningQuery = map { rma ⇒
    (rma.id, rma.referenceNumber)
  }
}
