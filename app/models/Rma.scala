package models

import scala.concurrent.ExecutionContext

import com.pellucid.sealerate
import models.Rma.{RmaType, Standard, Status, Pending}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ModelWithLockParameter, TableQueryWithLock, ADT, GenericTable}
import utils.Slick.implicits._

final case class Rma(id: Int = 0, referenceNumber: String, orderId: Int, rmaType: RmaType = Standard,
  status: Status = Pending, locked: Boolean = false, customerId: Option[Int] = None,
  storeAdminId: Option[Int] = None)
  extends ModelWithLockParameter {

  def isNew: Boolean = id == 0
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
  def rmaType = column[RmaType]("rma_type")
  def status = column[Status]("status")
  def locked = column[Boolean]("locked")
  def customerId = column[Option[Int]]("customer_id")
  def storeAdminId = column[Option[Int]]("store_admin_id")

  def * = (id, referenceNumber, orderId, rmaType, status, locked, customerId,
    storeAdminId) <> ((Rma.apply _).tupled, Rma.unapply)
}

object Rmas extends TableQueryWithLock[Rma, Rmas](
  idLens = GenLens[Rma](_.id)
)(new Rmas(_)) {

  val returningIdAndReferenceNumber = this.returning(map { rma ⇒ (rma.id, rma.referenceNumber) })

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


  def findByRefNum(refNum: String): QuerySeq =
    filter(_.referenceNumber === refNum)
}
