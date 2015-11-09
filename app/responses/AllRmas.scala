package responses

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import models._
import slick.driver.PostgresDriver.api._

object AllRmas {
  type Response = Future[Seq[Root]]

  val mockTotal = 50

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderId: Int,
    orderRefNum: String,
    rmaType: Rma.RmaType,
    status: Rma.Status,
    customer: CustomerResponse.Root,
    storeAdmin: Option[StoreAdminResponse.Root] = None,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Option[Instant] = None,
    total: Option[Int] = None
    ) extends ResponseItem

  def build(rma: Rma, customer: Customer, admin: Option[StoreAdmin] = None)
    (implicit ec: ExecutionContext): DBIO[Root] = {
    DBIO.successful(
      Root(
        id = rma.id,
        referenceNumber = rma.referenceNumber,
        orderId = rma.orderId,
        orderRefNum = rma.orderRefNum,
        rmaType = rma.rmaType,
        status = rma.status,
        customer = CustomerResponse.build(customer),
        storeAdmin = admin.map(StoreAdminResponse.build),
        createdAt = rma.createdAt,
        updatedAt = rma.updatedAt,
        deletedAt = rma.deletedAt,
        total = Some(mockTotal)
      )
    )
  }
}