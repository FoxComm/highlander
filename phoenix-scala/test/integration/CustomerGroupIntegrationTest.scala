import com.github.tminglei.slickpg.LTree
import failures.NotFoundFailure404
import models.customer.CustomerGroup._
import models.customer._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import org.scalatest.mockito.MockitoSugar
import payloads.CustomerGroupPayloads.CustomerGroupPayload
import responses.GroupResponses.GroupResponse.{Root, build}
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.seeds.Seeds.Factories
import shapeless._
import utils.db._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import cats.implicits._

class CustomerGroupIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockitoSugar
    with BakedFixtures {

  "POST /v1/customer-groups" - {
    "successfully creates customer group from payload" in new Fixture {
      val payload = CustomerGroupPayload(name = "Group number one",
                                         clientState = JObject(),
                                         elasticRequest = JObject(),
                                         customersCount = 1,
                                         groupType = Manual)

      val root    = customerGroupsApi.create(payload).as[Root]
      val created = CustomerGroups.mustFindById400(root.id).gimme
      created.id must === (root.id)
      created.groupType must === (Manual)
    }

    "successfully creates customer group and link to template" in new Fixture {
      val scopeN = "1"

      val payload = CustomerGroupPayload(name = "Group number one",
                                         clientState = JObject(),
                                         elasticRequest = JObject(),
                                         customersCount = 1,
                                         templateId = groupTemplate.id.some,
                                         scope = scopeN.some,
                                         groupType = Dynamic)

      val root    = customerGroupsApi.create(payload).as[Root]
      val created = CustomerGroups.mustFindById400(root.id).gimme
      created.id must === (root.id)
      created.groupType must === (Dynamic)
      val templateLink = GroupTemplateInstances
        .filter(_.groupId === root.id)
        .filter(_.groupTemplateId === groupTemplate.id)
        .mustFindOneOr(NotFoundFailure404(GroupTemplateInstances, "fakeId"))
        .gimme
      templateLink.scope must === (LTree(scopeN))
    }

    "inserts group query if elasticquery is empty" in new Fixture {
      val scopeN = "1"
      val payload = CustomerGroupPayload(name = "Group number one",
                                         clientState = JObject(),
                                         elasticRequest = JNull,
                                         customersCount = 1,
                                         scope = scopeN.some,
                                         groupType = Manual)

      val root = customerGroupsApi.create(payload).as[Root]
      root.elasticRequest must !==(JObject())
      ((((root.elasticRequest \ "query" \ "bool" \ "filter")(0) \ "bool" \ "must")(0) \ "term" \ "groups")) must === (
          JInt(root.id))
    }

    "fail to create customer group with nonexistnet tempalte id" in new Fixture {
      val payload = CustomerGroupPayload(name = "Group number one",
                                         clientState = JObject(),
                                         elasticRequest = JObject(),
                                         customersCount = 1,
                                         templateId = 666.some,
                                         groupType = Dynamic)

      customerGroupsApi
        .create(payload)
        .mustFailWith404(NotFoundFailure404(CustomerGroupTemplate, 666))
    }
  }

  "GET /v1/customer-groups/:groupId" - {
    "fetches group info" in new Fixture {
      customerGroupsApi(group.id).get().as[Root] must === (build(group))
    }

    "404 if group not found" in new Fixture {
      customerGroupsApi(999).get().mustFailWith404(NotFoundFailure404(CustomerGroup, 999))
    }
  }

  "PATCH /v1/customer-groups/:groupId" - {
    "successfully updates group attributes" in new Fixture {
      val payload = CustomerGroupPayload(name = "New name for group",
                                         customersCount = 777,
                                         clientState = JObject(),
                                         elasticRequest = JObject(),
                                         groupType = Dynamic)

      (payload.name, payload.customersCount) must !==((group.name, group.customersCount))

      val updated = customerGroupsApi(group.id).update(payload).as[Root]
      (updated.name, updated.customersCount) must === ((payload.name, payload.customersCount))
    }

    "404 if group not found" in new Fixture {
      val payload = CustomerGroupPayload(name = "New name for group",
                                         customersCount = 777,
                                         clientState = JObject(),
                                         elasticRequest = JObject(),
                                         groupType = Dynamic)

      customerGroupsApi(999)
        .update(payload)
        .mustFailWith404(NotFoundFailure404(CustomerGroup, 999))
    }
  }

  "DELETE /v1/customer-groups/:groupId" - {
    "successfully deletes group" in new Fixture {
      customerGroupsApi(group.id).delete.mustBeEmpty()

      withClue(s"Customer group with id ${group.id} exists:") {
        CustomerGroups
          .filter(_.id === group.id)
          .filter(_.deletedAt.isEmpty)
          .gimme
          .isEmpty must === (true)
      }

      val tis = GroupTemplateInstances
        .findByScopeAndGroupId(group.scope, group.id)
        .filter(_.deletedAt.isEmpty)
        .result
        .gimme
      withClue(s"Customer group template instances with for group ${group.id} exist:") {
        tis.isEmpty must === (true)
      }
    }

    "404 if group not found" in new Fixture {
      customerGroupsApi(999).delete.mustFailWith404(NotFoundFailure404(CustomerGroup, 999))
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val group = CustomerGroups
      .create(Factories.group(LTree("1")).copy(createdBy = storeAdmin.accountId))
      .gimme

    val groupTemplate = CustomerGroupTemplates.create(Factories.template).gimme
  }
}
