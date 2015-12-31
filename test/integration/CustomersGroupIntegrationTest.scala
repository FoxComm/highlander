import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import models.activity.ActivityContext
import models.{CustomerDynamicGroups, CustomerDynamicGroup, StoreAdmins}
import org.mockito.Mockito.{reset, when}
import org.mockito.{Matchers => m}
import org.scalatest.mock.MockitoSugar
import payloads.CustomerDynamicGroupPayload
import responses.CreditCardsResponse.{Root => CardResponse}
import responses.DynamicGroupResponse
import services.CreditCardFailure.StripeFailure
import services.orders.OrderPaymentUpdater
import services.{CannotUseInactiveCreditCard, CreditCardManager, CustomerEmailNotUnique, GeneralFailure, NotFoundFailure404, Result}
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.jdbc._
import utils.seeds.Seeds.Factories
import utils.seeds.SeedsGenerator.generateGroup
import Extensions._
import slick.driver.PostgresDriver.api._
import util.SlickSupport.implicits._
import org.json4s.JObject

import concurrent.ExecutionContext.Implicits.global

class CustomersGroupIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with SortingAndPaging[DynamicGroupResponse.Root]
  with MockitoSugar {

  import concurrent.ExecutionContext.Implicits.global

  def responseItems = {
    val admin = StoreAdmins.create(authedStoreAdmin).run().futureValue.rightVal

    val insertGroups = (1 to numOfResults).map { _ ⇒ generateGroup(admin.id) }
    val dbio = for {
      groups ← (CustomerDynamicGroups ++= insertGroups) >> CustomerDynamicGroups.result
    } yield groups.map(DynamicGroupResponse.build(_))

    dbio.transactionally.run().futureValue.toIndexedSeq
  }

  val sortColumnName = "name"

  def responseItemsSort(items: IndexedSeq[DynamicGroupResponse.Root]) = items.sortBy(_.name)

  def mf = implicitly[scala.reflect.Manifest[DynamicGroupResponse.Root]]
  // paging and sorting API end

  val uriPrefix = "v1/groups"

  "GET /v1/groups" - {
    "lists customers groups" in new Fixture {
      val response = GET(s"$uriPrefix")
      val groupRoot = DynamicGroupResponse.build(group)

      response.status must === (StatusCodes.OK)
      response.as[DynamicGroupResponse.Root#ResponseMetadataSeq].result must === (Seq(groupRoot))
    }
  }

  "POST /v1/groups" - {
    "successfully creates customer group from payload" in new Fixture {
      val response = POST(s"v1/groups", CustomerDynamicGroupPayload(name = "Group number one",
        clientState = JObject(), elasticRequest = JObject(), customersCount = Some(1)))

      response.status must ===(StatusCodes.OK)

      val root = response.as[DynamicGroupResponse.Root]
      val created = CustomerDynamicGroups.findOneById(root.id).run().futureValue.value
      created.id must === (root.id)
    }
  }

  "GET /v1/groups/:groupId" - {
    "fetches group info" in new Fixture {
      val response = GET(s"$uriPrefix/${group.id}")
      val root = DynamicGroupResponse.build(group)

      response.status must === (StatusCodes.OK)
      response.as[DynamicGroupResponse.Root] must === (root)
    }
  }

  "PATCH /v1/groups/:groupId" - {
    "successfully updates group attributes" in new Fixture {
      val payload = CustomerDynamicGroupPayload(name = "New name for group", customersCount = Some(777),
            clientState = JObject(), elasticRequest = JObject())
      (payload.name, payload.customersCount) must !== ((group.name, group.customersCount))

      val response = PATCH(s"v1/groups/${group.id}", payload)
      response.status must === (StatusCodes.OK)

      val updated = response.as[DynamicGroupResponse.Root]
      (updated.name, updated.customersCount) must === ((payload.name, payload.customersCount))
    }
  }

  trait Fixture {
    val (group, admin) = (for {
      admin ← * <~ StoreAdmins.create(authedStoreAdmin)
      group ← * <~ CustomerDynamicGroups.create(Factories.group.copy(createdBy = admin.id))
    } yield (group, admin)).runT().futureValue.rightVal
  }
}