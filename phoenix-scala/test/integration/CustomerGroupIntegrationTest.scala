import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.NotFoundFailure404
import models.customer.{CustomerDynamicGroup, CustomerDynamicGroups}
import org.json4s.JObject
import org.scalatest.mock.MockitoSugar
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import responses.DynamicGroupResponse
import responses.DynamicGroupResponse.Root
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CustomerGroupIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockitoSugar
    with BakedFixtures {

  "GET /v1/groups" - {
    "lists customers groups" in new Fixture {
      val groupRoot = DynamicGroupResponse.build(group)

      customerGroupsApi.get().as[Seq[Root]] must === (Seq(groupRoot))
    }
  }

  "POST /v1/groups" - {
    "successfully creates customer group from payload" in new Fixture {
      val payload = CustomerDynamicGroupPayload(name = "Group number one",
                                                clientState = JObject(),
                                                elasticRequest = JObject(),
                                                customersCount = Some(1))
      val root    = customerGroupsApi.create(payload).as[Root]
      val created = CustomerDynamicGroups.mustFindById400(root.id).gimme
      created.id must === (root.id)
    }
  }

  "GET /v1/groups/:groupId" - {
    "fetches group info" in new Fixture {
      customerGroupsApi(group.id).get().as[Root] must === (DynamicGroupResponse.build(group))
    }

    "404 if group not found" in new Fixture {
      val response = customerGroupsApi(999).get()

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CustomerDynamicGroup, 999).description)
    }
  }

  "PATCH /v1/groups/:groupId" - {
    "successfully updates group attributes" in new Fixture {
      val payload = CustomerDynamicGroupPayload(name = "New name for group",
                                                customersCount = Some(777),
                                                clientState = JObject(),
                                                elasticRequest = JObject())
      (payload.name, payload.customersCount) must !==((group.name, group.customersCount))

      val updated = customerGroupsApi(group.id).update(payload).as[Root]
      (updated.name, updated.customersCount) must === ((payload.name, payload.customersCount))
    }

    "404 if group not found" in new Fixture {
      val payload = CustomerDynamicGroupPayload(name = "New name for group",
                                                customersCount = Some(777),
                                                clientState = JObject(),
                                                elasticRequest = JObject())
      val response = customerGroupsApi(999).update(payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CustomerDynamicGroup, 999).description)
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val group = CustomerDynamicGroups.create(Factories.group.copy(createdBy = storeAdmin.id)).gimme
  }
}
