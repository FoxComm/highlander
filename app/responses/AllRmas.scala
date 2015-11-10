package responses

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import models._
import slick.driver.PostgresDriver.api._
import utils.Slick._

object AllRmas {
  type Response = Future[Seq[Root]]

  val mockTotal = 50

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderRefNum: String,
    rmaType: Rma.RmaType,
    status: Rma.Status,
    customer: CustomerResponse.Root,
    storeAdmin: Option[StoreAdminResponse.Root] = None,
    assignees: Seq[AssignmentResponse.Root],
    createdAt: Instant,
    updatedAt: Instant,
    total: Option[Int] = None
    ) extends ResponseItem

  def build(rma: Rma, customer: Customer, admin: Option[StoreAdmin] = None)
    (implicit ec: ExecutionContext, db: Database): DBIO[Root] = {
    fetchAssignees(rma).map { case (assignments) ⇒
      Root(
        id = rma.id,
        referenceNumber = rma.referenceNumber,
        orderRefNum = rma.orderRefNum,
        rmaType = rma.rmaType,
        status = rma.status,
        customer = CustomerResponse.build(customer),
        storeAdmin = admin.map(StoreAdminResponse.build),
        assignees = assignments.map((AssignmentResponse.buildForRma _).tupled),
        createdAt = rma.createdAt,
        updatedAt = rma.updatedAt,
        total = Some(mockTotal)
      )
    }
  }

  private def fetchAssignees(rma: Rma)(implicit ec: ExecutionContext, db: Database) = {
    for {
      assignments ← RmaAssignments.filter(_.rmaId === rma.id).result
      admins ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result
    } yield assignments.zip(admins)
  }
}