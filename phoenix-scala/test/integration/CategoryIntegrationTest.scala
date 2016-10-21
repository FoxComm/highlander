import org.json4s.JsonAST.{JObject, JString}
import org.json4s.JsonDSL._
import payloads.CategoryPayloads._
import responses.CategoryResponses._
import services.category.CategoryManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.aliases._

class CategoryIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
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
        val newAttribute      = "attr2"
        val newValue          = JString("val2")
        val updatedAttributes = newAttribute → newValue

        val content = categoriesApi(category.form.id)
          .update(UpdateFullCategory(UpdateCategoryForm(updatedAttributes),
                                     UpdateCategoryShadow(updatedAttributes)))
          .as[FullCategoryResponse.Root]

        val expectedFormValues: List[JString] =
          newValue :: testAttributes.map { case (_, value) ⇒ value }
        val formValues: List[Json] = content.form.attributes.asInstanceOf[JObject].children
        val shadowKeys: Iterable[String] =
          content.shadow.attributes.asInstanceOf[JObject].values.keys

        formValues must contain only (expectedFormValues: _*)
        shadowKeys must contain(newAttribute)
      }
    }

    "POST v1/categories/:context" - {
      "creates a category" in {
        val newAttribute      = "attr2"
        val newValue          = JString("val2")
        val updatedAttributes = newAttribute → newValue

        val content = categoriesApi
          .create(CreateFullCategory(CreateCategoryForm(updatedAttributes),
                                     CreateCategoryShadow(updatedAttributes)))
          .as[FullCategoryResponse.Root]

        val formValues: List[Json] = content.form.attributes.asInstanceOf[JObject].children
        val shadowKeys: Iterable[String] =
          content.shadow.attributes.asInstanceOf[JObject].values.keys

        formValues must contain only newValue
        shadowKeys must contain(newAttribute)
      }
    }

    "GET v1/categories/:formId/form" - {
      "return form" in new Fixture {
        val content = categoriesApi(category.form.id).form().as[CategoryFormResponse.Root]

        val expectedFormValues: List[JString] = testAttributes.map {
          case (_, value) ⇒ value
        }
        val formValues: List[Json] = content.attributes.asInstanceOf[JObject].children

        formValues must contain only (expectedFormValues: _*)
        content.id must === (category.form.id)
      }
    }

    "GET v1/categories/:context/:formId/baked" - {
      "returns illuminated object" in new Fixture {
        val content = categoriesApi(category.form.id).baked().as[IlluminatedCategoryResponse.Root]

        val expected: JObject = testAttributes.map {
          case (key, value) ⇒ key → JObject(List("v" → value))
        }
        content.attributes must === (expected)
      }
    }

    "GET v1/categories/:context/:formId/shadow" - {
      "returns shadow" in new Fixture {
        val keys = categoriesApi(category.form.id)
          .shadow()
          .as[CategoryShadowResponse.Root]
          .attributes
          .asInstanceOf[JObject]
          .values
          .keys

        keys must contain only (testAttributes.map { case (key, _) ⇒ key }: _*)
      }
    }

    trait Fixture extends StoreAdmin_Seed {
      val testAttributes = List("attr1" → JString("val1"))

      val category = CategoryManager
        .createCategory(storeAdmin,
                        CreateFullCategory(CreateCategoryForm(testAttributes),
                                           CreateCategoryShadow(testAttributes)),
                        ctx.name)
        .gimme
    }
  }
}
