import com.github.tminglei.slickpg.LTree
import failures.NotFoundFailure404
import models.customer._
import org.json4s.JObject
import org.scalatest.mock.MockitoSugar
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import responses.DynamicGroupResponse
import responses.DynamicGroupResponse.Root
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.seeds.Seeds.Factories
import shapeless._
import utils.db._
import utils.aliases._
import utils.db.ExPostgresDriver.api._

class CustomerGroupIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockitoSugar
    with BakedFixtures {

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

    "successfully creates customer group and link to template" in new Fixture {
      val scopeN = "1"
      val payload = CustomerDynamicGroupPayload(name = "Group number one",
                                                clientState = JObject(),
                                                elasticRequest = JObject(),
                                                customersCount = Some(1),
                                                templateId = Some(groupTemplate.id),
                                                scope = Some(scopeN))

      val root    = customerGroupsApi.create(payload).as[Root]
      val created = CustomerDynamicGroups.mustFindById400(root.id).gimme
      created.id must === (root.id)
      val templateLink = GroupTemplateInstances
        .filter(_.groupId === root.id)
        .filter(_.groupTemplateId === groupTemplate.id)
        .mustFindOneOr(NotFoundFailure404(GroupTemplateInstances, "fakeId"))
        .gimme
      templateLink.scope must === (LTree(scopeN))
    }

    "fail to create customer group with nonexistnet tempalte id" in new Fixture {
      val payload = CustomerDynamicGroupPayload(name = "Group number one",
                                                clientState = JObject(),
                                                elasticRequest = JObject(),
                                                customersCount = Some(1),
                                                templateId = Some(666))

      customerGroupsApi
        .create(payload)
        .mustFailWith404(NotFoundFailure404(CustomerGroupTemplate, 666))
    }
  }

  "GET /v1/groups/:groupId" - {
    "fetches group info" in new Fixture {
      customerGroupsApi(group.id).get().as[Root] must === (DynamicGroupResponse.build(group))
    }

    "404 if group not found" in new Fixture {
      customerGroupsApi(999).get().mustFailWith404(NotFoundFailure404(CustomerDynamicGroup, 999))
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
      customerGroupsApi(999)
        .update(payload)
        .mustFailWith404(NotFoundFailure404(CustomerDynamicGroup, 999))
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val group = CustomerDynamicGroups
      .create(Factories.group(LTree("1")).copy(createdBy = storeAdmin.accountId))
      .gimme

    val groupTemplate = CustomerGroupTemplates.create(Factories.template).gimme
  }
}
