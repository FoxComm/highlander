package models

import scala.concurrent.ExecutionContext

import com.pellucid.sealerate
import models.Rma.{RmaType, Standard, Status, Pending}
import monocle.macros.GenLens

import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.CustomDirectives.SortAndPage

import utils.{ModelWithLockParameter, TableQueryWithLock, ADT, GenericTable}
import utils.Slick.implicits._

final case class Rma(id: Int = 0, referenceNumber: String = "", orderId: Int, orderRefNum: String,
  rmaType: RmaType = Standard, status: Status = Pending, locked: Boolean = false,
  customerId: Option[Int] = None, storeAdminId: Option[Int] = None)
  extends ModelWithLockParameter {

  def refNum: String = referenceNumber
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

  val rmaRefNumRegex = """([a-zA-Z0-9-_]*)""".r
}

class Rmas(tag: Tag) extends GenericTable.TableWithLock[Rma](tag, "rmas")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def orderId = column[Int]("order_id")
  def orderRefNum = column[String]("order_refnum")
  def rmaType = column[RmaType]("rma_type")
  def status = column[Status]("status")
  def locked = column[Boolean]("locked")
  def customerId = column[Option[Int]]("customer_id")
  def storeAdminId = column[Option[Int]]("store_admin_id")

  def * = (id, referenceNumber, orderId, orderRefNum, rmaType, status, locked, customerId,
    storeAdminId) <> ((Rma.apply _).tupled, Rma.unapply)
}

object Rmas extends TableQueryWithLock[Rma, Rmas](
  idLens = GenLens[Rma](_.id)
)(new Rmas(_)) {

  override def primarySearchTerm: String = "referenceNumber"

  val returningIdAndReferenceNumber = this.returning(map { rma ⇒ (rma.id, rma.referenceNumber) })

  def sortedAndPaged(query: QuerySeq)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata = {
    query.withMetadata.sortAndPageIfNeeded { (s, rma) ⇒
      s.sortColumn match {
        case "id"               ⇒ if (s.asc) rma.id.asc               else rma.id.desc
        case "referenceNumber"  ⇒ if (s.asc) rma.referenceNumber.asc  else rma.referenceNumber.desc
        case "orderId"          ⇒ if (s.asc) rma.orderId.asc          else rma.orderId.desc
        case "orderRefNum"      ⇒ if (s.asc) rma.orderRefNum.asc      else rma.orderRefNum.desc
        case "rmaType"          ⇒ if (s.asc) rma.rmaType.asc          else rma.rmaType.desc
        case "status"           ⇒ if (s.asc) rma.status.asc           else rma.status.desc
        case "locked"           ⇒ if (s.asc) rma.locked.asc           else rma.locked.desc
        case "customerId"       ⇒ if (s.asc) rma.customerId.asc       else rma.customerId.desc
        case "storeAdminId"     ⇒ if (s.asc) rma.storeAdminId.asc     else rma.storeAdminId.desc
        case other              ⇒ invalidSortColumn(other)
      }
    }
  }

  def queryAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata =
    sortedAndPaged(this)

  def queryByOrderRefNum(refNum: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata =
    sortedAndPaged(findByOrderRefNum(refNum))

  def queryByCustomerId(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata =
    sortedAndPaged(findByCustomerId(customerId))

  override def save(rma: Rma)(implicit ec: ExecutionContext) = {
    if (rma.isNew) {
      create(rma)
    } else {
      super.save(rma)
    }
  }

  def create(rma: Rma)(implicit ec: ExecutionContext): DBIO[models.Rma] = for {
    (newId, refNum) ← returningIdAndReferenceNumber += rma
  } yield rma.copy(id = newId, referenceNumber = refNum)

  def findByRefNum(refNum: String): QuerySeq = filter(_.referenceNumber === refNum)

  def findByCustomerId(customerId: Int): QuerySeq = filter(_.customerId === customerId)

  def findByOrderRefNum(refNum: String): QuerySeq = filter(_.orderRefNum === refNum)
}
