import cats.implicits._
import org.json4s.JObject
import org.scalatest.mockito.MockitoSugar
import phoenix.models.customer.CustomerGroup._
import phoenix.models.customer.{CustomerGroupTemplate, CustomerGroupTemplates}
import phoenix.payloads.CustomerGroupPayloads.CustomerGroupPayload
import phoenix.responses.GroupResponses.GroupResponse.Root
import phoenix.utils.seeds.Factories
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

class CustomerGroupTemplateIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with MockitoSugar
    with BakedFixtures {

  "sends full list of templates when templates are not used" in new Fixture {
    customerGroupTemplateApi.get().as[Seq[CustomerGroupTemplate]] must === (groupTemplates)
  }

  "sends unused templates only when templates are not used" in new Fixture {
    val scopeN = "1"

    val payload = CustomerGroupPayload(
      name = "Group number one",
      clientState = JObject(),
      elasticRequest = JObject(),
      customersCount = 1,
      templateId = groupTemplates.head.id.some,
      scope = scopeN.some,
      groupType = Template
    )

    val root = customerGroupsApi.create(payload).as[Root]

    val expected = groupTemplates.tail
    customerGroupTemplateApi.get().as[Seq[CustomerGroupTemplate]] must === (expected)
  }

  trait Fixture extends StoreAdmin_Seed {

    val groupTemplates = Factories.templates.map(CustomerGroupTemplates.create(_).gimme)

  }

}
