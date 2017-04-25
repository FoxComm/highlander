import io.circe._
import payloads.CategoryPayloads._
import responses.CategoryResponses._
import services.category.CategoryManager
import testutils.PayloadHelpers._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.aliases._
import utils.json.yolo._

class CategoryIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with TestObjectContext
    with BakedFixtures {

  "Categories" - {
    "GET v1/categories/:context/:formId" - {
      "returns a full category" in new Fixture {
        val content = categoriesApi(category.form.id).get().as[FullCategoryResponse.Root]

        content.form.id must === (category.form.id)
        content.shadow.id must === (category.shadow.id)
      }
    }

    "PATCH v1/categories/:context/:formId" - {
      "returns a full category" in new Fixture {
        val newAttribute  = "attr2"
        val newValue      = Json.fromString("val2")
        val updatedForm   = Json.obj(newAttribute → newValue)
        val updatedShadow = Json.obj(newAttribute → Json.obj("type" → Json.fromString("string")))

        val content = categoriesApi(category.form.id)
          .update(UpdateFullCategory(UpdateCategoryForm(updatedForm),
                                     UpdateCategoryShadow(updatedShadow)))
          .as[FullCategoryResponse.Root]

        val formValues = content.form.attributes.extract[JsonObject].values
        val shadowKeys = content.shadow.attributes.extract[JsonObject].fields

        formValues must contain only (testAttributesMod.extract[JsonObject].values: _*)
        shadowKeys must contain(newAttribute)
      }
    }

    "POST v1/categories/:context" - {
      "creates a category" in {
        val newAttribute  = "attr2"
        val newValue      = Json.fromString("val2")
        val updatedForm   = Json.obj(newAttribute → newValue)
        val updatedShadow = Json.obj(newAttribute → Json.obj("type" → Json.fromString("string")))

        val content = categoriesApi
          .create(CreateFullCategory(CreateCategoryForm(updatedForm),
                                     CreateCategoryShadow(updatedShadow)))
          .as[FullCategoryResponse.Root]

        val formValues = content.form.attributes.extract[JsonObject].values
        val shadowKeys = content.shadow.attributes.extract[JsonObject].fields

        formValues must contain only newValue
        shadowKeys must contain(newAttribute)
      }
    }

    "GET v1/categories/:formId/form" - {
      "return form" in new Fixture {
        val content = categoriesApi(category.form.id).form().as[CategoryFormResponse.Root]

        val formValues = content.form.attributes.extract[JsonObject].value

        formValues must contain only (testAttributes.extract[JsonObject].values: _*)
        content.id must === (category.form.id)
      }
    }

    "GET v1/categories/:context/:formId/baked" - {
      "returns illuminated object" in new Fixture {
        val content = categoriesApi(category.form.id).baked().as[IlluminatedCategoryResponse.Root]

        val expected = testAttributes.extract[JsonObject].withJsons(tv(_))
        content.attributes.asObject.value must === (expected)
      }
    }

    "GET v1/categories/:context/:formId/shadow" - {
      "returns shadow" in new Fixture {
        val keys = categoriesApi(category.form.id)
          .shadow()
          .as[CategoryShadowResponse.Root]
          .attributes
          .extract[JsonObject]
          .fields

        keys must contain only (testAttributes.extract[JsonObject].values: _*)
      }
    }

    trait Fixture extends StoreAdmin_Seed {
      val testAttributes: Json       = Json.obj("attr1" -> Json.fromString("val1"))
      val testAttributesMod: Json    = Json.obj("attr1" -> Json.fromString("val1"), "attr2" -> Json.fromString("val2"))
      val testShadowAttributes: Json = Json.obj("attr1" -> Json.obj("type" → Json.fromString("string")))

      val category = CategoryManager
        .createCategory(storeAdmin,
                        CreateFullCategory(CreateCategoryForm(testAttributes),
                                           CreateCategoryShadow(testShadowAttributes)),
                        ctx.name)
        .gimme
    }
  }
}
