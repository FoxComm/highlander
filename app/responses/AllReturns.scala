package responses

import java.time.Instant

import scala.concurrent.Future

import models.customer.Customer
import models.returns.Return
import models.StoreAdmin

object AllReturns {
  type Response = Future[Seq[Root]]

  val mockTotal = 50

  case class Root(
      id: Int,
      referenceNumber: String,
      orderRefNum: String,
      rmaType: Return.ReturnType,
      state: Return.State,
      customer: Option[CustomerResponse.Root] = None,
      storeAdmin: Option[StoreAdminResponse.Root] = None,
      createdAt: Instant,
      updatedAt: Instant,
      total: Option[Int] = None
  ) extends ResponseItem

  def build(rma: Return,
            customer: Option[Customer] = None,
            admin: Option[StoreAdmin] = None): Root =
    Root(
        id = rma.id,
        referenceNumber = rma.referenceNumber,
        orderRefNum = rma.orderRefNum,
        rmaType = rma.returnType,
        state = rma.state,
        customer = customer.map(CustomerResponse.build(_)),
        storeAdmin = admin.map(StoreAdminResponse.build),
        createdAt = rma.createdAt,
        updatedAt = rma.updatedAt,
        total = Some(mockTotal)
    )
}
