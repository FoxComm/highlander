package responses

import java.time.Instant

import scala.concurrent.Future

import models.customer.Customer
import models.rma.Rma
import models.StoreAdmin

object AllRmas {
  type Response = Future[Seq[Root]]

  val mockTotal = 50

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderRefNum: String,
    rmaType: Rma.RmaType,
    state: Rma.State,
    customer: Option[CustomerResponse.Root] = None,
    storeAdmin: Option[StoreAdminResponse.Root] = None,
    createdAt: Instant,
    updatedAt: Instant,
    total: Option[Int] = None
  ) extends ResponseItem

  def build(rma: Rma, customer: Option[Customer] = None, admin: Option[StoreAdmin] = None): Root = Root(
    id = rma.id,
    referenceNumber = rma.referenceNumber,
    orderRefNum = rma.orderRefNum,
    rmaType = rma.rmaType,
    state = rma.state,
    customer = customer.map(CustomerResponse.build(_)),
    storeAdmin = admin.map(StoreAdminResponse.build),
    createdAt = rma.createdAt,
    updatedAt = rma.updatedAt,
    total = Some(mockTotal)
  )
}