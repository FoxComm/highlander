import java.time.Instant

import Extensions._
import akka.http.scaladsl.model.StatusCodes
import cats.data.Xor
import models.customer.Customers
import models.order.{Orders, Order}
import models.rma.{Rmas, Rma}
import models.{StoreAdmins, StoreAdmin}
import responses.{AllRmas, RmaResponse, StoreAdminResponse, BatchResponse}
import services.NotFoundFailure404
import services.rmas._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds.Factories
import utils.seeds.RankingSeedsGenerator
import utils.time._

import scala.concurrent.ExecutionContext.Implicits.global

class AllRmasIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with SortingAndPaging[AllRmas.Root]
  with AutomaticAuth {

  // paging and sorting API
  def uriPrefix = "v1/rmas"

  def responseItems = {
    val orderRefNum = RankingSeedsGenerator.randomString(10)

    val dbio = for {
      customer ← * <~ Customers.create(RankingSeedsGenerator.generateCustomer)
      order ← * <~ Orders.create(Factories.order.copy(
        customerId = customer.id,
        referenceNumber = orderRefNum,
        state = Order.RemorseHold,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))

      insertRmas = (1 to numOfResults).map { i ⇒
        Factories.rma.copy(
          customerId = customer.id,
          orderId = order.id,
          orderRefNum = orderRefNum,
          referenceNumber = s"RMA-$i"
        )
      }

      _ ← * <~ Rmas.createAll(insertRmas)
    } yield ()

    dbio.runTxn().futureValue
    getAllRmas.toIndexedSeq
  }

  val sortColumnName = "referenceNumber"

  def responseItemsSort(items: IndexedSeq[AllRmas.Root]) = items.sortBy(_.referenceNumber)

  def mf = implicitly[scala.reflect.Manifest[AllRmas.Root]]
  // paging and sorting API end

  def getAllRmas: Seq[AllRmas.Root] = {
    RmaQueries.findAll(Rmas).futureValue match {
      case Xor.Left(s)    ⇒ fail(s.toList.mkString(";"))
      case Xor.Right(seq) ⇒ seq.result
    }
  }
}
