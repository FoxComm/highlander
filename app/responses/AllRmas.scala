package responses

import java.time.Instant

import scala.concurrent.Future

import models.customer.Customer
import models.rma.{Rma, RmaAssignments}
import models.{StoreAdmin, StoreAdmins}
import slick.driver.PostgresDriver.api._
import utils.aliases._

object AllRmas {
  type Response = Future[Seq[Root]]

  val mockTotal = 50

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderRefNum: String,
    rmaType: Rma.RmaType,
    state: Rma.State,
    customer: CustomerResponse.Root,
    storeAdmin: Option[StoreAdminResponse.Root] = None,
    assignees: Seq[AssignmentResponse.Root],
    createdAt: Instant,
    updatedAt: Instant,
    total: Option[Int] = None
  ) extends ResponseItem

  def fromRma(rma: Rma, customer: Customer, admin: Option[StoreAdmin] = None)
    (implicit ec: EC, db: DB): DBIO[Root] = {
    fetchAssignees(rma).map { case (assignees) ⇒
      build(
        rma = rma,
        customer = customer,
        admin = admin,
        assignees = assignees.map((AssignmentResponse.buildForRma _).tupled)
      )
    }
  }

  def build(rma: Rma, customer: Customer, admin: Option[StoreAdmin] = None,
    assignees: Seq[AssignmentResponse.Root] = Seq.empty): Root  = {
    Root(
      id = rma.id,
      referenceNumber = rma.referenceNumber,
      orderRefNum = rma.orderRefNum,
      rmaType = rma.rmaType,
      state = rma.state,
      customer = CustomerResponse.build(customer),
      storeAdmin = admin.map(StoreAdminResponse.build),
      assignees = assignees,
      createdAt = rma.createdAt,
      updatedAt = rma.updatedAt,
      total = Some(mockTotal)
    )
  }

  private def fetchAssignees(rma: Rma)(implicit ec: EC, db: DB) = {
    for {
      assignments ← RmaAssignments.filter(_.rmaId === rma.id).result
      admins ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result
    } yield assignments.zip(admins)
  }
}