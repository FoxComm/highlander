package responses

import java.time.Instant

import scala.concurrent.Future

import models.account.User
import models.admin.AdminData
import models.customer.CustomerData
import models.returns.Return

object AllReturns {
  type Response = Future[Seq[Root]]

  val mockTotal = 50

  case class Root(
      id: Int,
      referenceNumber: String,
      cordRefNum: String,
      rmaType: Return.ReturnType,
      state: Return.State,
      customer: Option[CustomerResponse.Root] = None,
      storeAdmin: Option[StoreAdminResponse.Root] = None,
      createdAt: Instant,
      updatedAt: Instant,
      total: Option[Int] = None
  ) extends ResponseItem

  def build(rma: Return,
            customer: Option[User] = None,
            customerData: Option[CustomerData] = None,
            admin: Option[User] = None,
            adminData: Option[AdminData] = None): Root =
    Root(
      id = rma.id,
      referenceNumber = rma.referenceNumber,
      cordRefNum = rma.orderRef,
      rmaType = rma.returnType,
      state = rma.state,
      customer = for {
        c  ← customer
        cu ← customerData
      } yield CustomerResponse.build(c, cu),
      storeAdmin = for {
        a  ← admin
        au ← adminData
      } yield StoreAdminResponse.build(a, au),
      createdAt = rma.createdAt,
      updatedAt = rma.updatedAt,
      total = Some(mockTotal)
    )
}
