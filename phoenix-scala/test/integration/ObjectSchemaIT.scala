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

  "GET v1/objects/schemas/:context/:kind" - {
    "Returns a product schema" in new ProductAndSkus_Baked {
      val result = schemasApi("default").get("product")
      result.mustBeOk()

      val response = result.as[Root]
      response.name === ("product")
    }

    "Responds with NOT FOUND when invalid kind is requested" in new ProductSku_ApiFixture {
      val kind = "not-real"
      schemasApi("default")
        .get(kind)
        .mustFailWith404(NotFoundFailure404(ObjectFullSchema, "kind", kind))
    }
  }
}
