import failures._
import models.objects._
import responses.ObjectResponses.ObjectSchemaResponse._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtures

class ObjectSchemaIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures
    with ApiFixtures {

  "GET v1/object/schemas/byName/:name" - {
    "Returns a product schema" in new ProductAndSkus_Baked {
      val result = schemasApi.getByKind("product")
      result.mustBeOk()

      val response = result.as[Seq[Root]]
      response.length must === (1)
      response.head.name === ("product")
    }

    "Responds with NOT FOUND when invalid kind is requested" in new ProductVariant_ApiFixture {
      val result = schemasApi.getByKind("not-real")
      result.mustBeOk()

      val response = result.as[Seq[Root]]
      response.length === (0)
    }
  }
}
