import models.customer.{CustomerGroupTemplate, CustomerGroupTemplates}
import org.json4s.JObject
import org.scalatest.mockito.MockitoSugar
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import responses.DynamicGroupResponses.DynamicGroupResponse.Root
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.seeds.Seeds.Factories

class CustomerGroupTemplateIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockitoSugar
    with BakedFixtures {

  "sends full list of templates when templates are not used" in new Fixture {
    customerGroupTemplateApi.get().as[Seq[CustomerGroupTemplate]] must === (groupTemplates)
  }

  "sends unused templates only when templates are not used" in new Fixture {
    val scopeN = "1"
    val payload = CustomerDynamicGroupPayload(name = "Group number one",
                                              clientState = JObject(),
                                              elasticRequest = JObject(),
                                              customersCount = Some(1),
                                              templateId = Some(groupTemplates.head.id),
                                              scope = Some(scopeN))

    val root = customerGroupsApi.create(payload).as[Root]

    val expected = groupTemplates.tail
    customerGroupTemplateApi.get().as[Seq[CustomerGroupTemplate]] must === (expected)
  }

  trait Fixture extends StoreAdmin_Seed {

    val groupTemplates = Factories.templates.map(CustomerGroupTemplates.create(_).gimme)

  }

}
