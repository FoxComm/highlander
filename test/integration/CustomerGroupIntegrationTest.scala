import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.NotFoundFailure404
import models.StoreAdmins
import models.customer.{CustomerDynamicGroup, CustomerDynamicGroups}
import org.json4s.JObject
import org.mockito.{Matchers ⇒ m}
import org.scalatest.mock.MockitoSugar
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import responses.CreditCardsResponse.{Root ⇒ CardResponse}
import responses.DynamicGroupResponse
import util.IntegrationTestBase
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories

class CustomerGroupIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with MockitoSugar {

  import concurrent.ExecutionContext.Implicits.global

  "GET /v1/groups" - {
    "lists customers groups" in new Fixture {
      val response  = GET("v1/groups")
      val groupRoot = DynamicGroupResponse.build(group)

      response.status must ===(StatusCodes.OK)
      response.ignoreFailuresAndGiveMe[Seq[DynamicGroupResponse.Root]] must ===(Seq(groupRoot))
    }
  }

  "POST /v1/groups" - {
    "successfully creates customer group from payload" in new Fixture {
      val payload = CustomerDynamicGroupPayload(name = "Group number one",
                                                clientState = JObject(),
                                                elasticRequest = JObject(),
                                                customersCount = Some(1))
      val response = POST(s"v1/groups", payload)

      response.status must ===(StatusCodes.OK)

      val root    = response.as[DynamicGroupResponse.Root]
      val created = CustomerDynamicGroups.findOneById(root.id).run().futureValue.value
      created.id must ===(root.id)
    }
  }

  "GET /v1/groups/:groupId" - {
    "fetches group info" in new Fixture {
      val response = GET(s"v1/groups/${group.id}")
      val root     = DynamicGroupResponse.build(group)

      response.status must ===(StatusCodes.OK)
      response.as[DynamicGroupResponse.Root] must ===(root)
    }

    "404 if group not found" in new Fixture {
      val response = GET("v1/groups/999")

      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(CustomerDynamicGroup, 999).description)
    }
  }

  "PATCH /v1/groups/:groupId" - {
    "successfully updates group attributes" in new Fixture {
      val payload = CustomerDynamicGroupPayload(name = "New name for group",
                                                customersCount = Some(777),
                                                clientState = JObject(),
                                                elasticRequest = JObject())
      (payload.name, payload.customersCount) must !==((group.name, group.customersCount))

      val response = PATCH(s"v1/groups/${group.id}", payload)
      response.status must ===(StatusCodes.OK)

      val updated = response.as[DynamicGroupResponse.Root]
      (updated.name, updated.customersCount) must ===((payload.name, payload.customersCount))
    }

    "404 if group not found" in new Fixture {
      val payload = CustomerDynamicGroupPayload(name = "New name for group",
                                                customersCount = Some(777),
                                                clientState = JObject(),
                                                elasticRequest = JObject())
      val response = PATCH("v1/groups/999", payload)

      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(CustomerDynamicGroup, 999).description)
    }
  }

  trait Fixture {
    val (group, admin) = (for {
      admin ← * <~ StoreAdmins.create(authedStoreAdmin)
      group ← * <~ CustomerDynamicGroups.create(Factories.group.copy(createdBy = admin.id))
    } yield (group, admin)).runTxn().futureValue.rightVal
  }
}
