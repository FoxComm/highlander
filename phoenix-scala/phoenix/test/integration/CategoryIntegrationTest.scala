import org.json4s.JsonAST.{JObject, JString}
import org.json4s.JsonDSL._
import phoenix.payloads.CategoryPayloads._
import phoenix.responses.CategoryResponses._
import phoenix.services.category.CategoryManager
import phoenix.utils.aliases._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

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
        val content = categoriesApi(category.form.id).get().as[FullCategoryResponse]

        content.form.id must === (category.form.id)
        content.shadow.id must === (category.shadow.id)
      }
    }

    "PATCH v1/categories/:context/:formId" - {
      "returns a full category" in new Fixture {
        val newAttribute           = "attr2"
        val newValue               = JString("val2")
        val updatedForm: JObject   = (newAttribute → newValue)
        val updatedShadow: JObject = (newAttribute → ("type" → "string"))

        val content = categoriesApi(category.form.id)
          .update(UpdateFullCategory(UpdateCategoryForm(updatedForm), UpdateCategoryShadow(updatedShadow)))
          .as[FullCategoryResponse]

        val formValues: List[Json] = content.form.attributes.asInstanceOf[JObject].children
        val shadowKeys: Iterable[String] =
          content.shadow.attributes.asInstanceOf[JObject].values.keys

        formValues must contain only (testAttributesMod.obj.map { case (k, v) ⇒ v }: _*)
        shadowKeys must contain(newAttribute)
      }
    }

    "POST v1/categories/:context" - {
      "creates a category" in {
        val newAttribute           = "attr2"
        val newValue               = JString("val2")
        val updatedForm: JObject   = (newAttribute → newValue)
        val updatedShadow: JObject = (newAttribute → ("type" → "string"))

        val content = categoriesApi
          .create(CreateFullCategory(CreateCategoryForm(updatedForm), CreateCategoryShadow(updatedShadow)))
          .as[FullCategoryResponse]

        val formValues: List[Json] = content.form.attributes.asInstanceOf[JObject].children
        val shadowKeys: Iterable[String] =
          content.shadow.attributes.asInstanceOf[JObject].values.keys

        formValues must contain only newValue
        shadowKeys must contain(newAttribute)
      }
    }

    "GET v1/categories/:formId/form" - {
      "return form" in new Fixture {
        val content = categoriesApi(category.form.id).form().as[CategoryFormResponse]

        val formValues: List[Json] = content.attributes.asInstanceOf[JObject].children

        formValues must contain only (testAttributes.obj.map { case (k, v) ⇒ v }: _*)
        content.id must === (category.form.id)
      }
    }

    "GET v1/categories/:context/:formId/baked" - {
      "returns illuminated object" in new Fixture {
        val content = categoriesApi(category.form.id).baked().as[IlluminatedCategoryResponse]

        val expected: JObject = testAttributes.obj.map {
          case (key, value) ⇒ (key, ("t" → "string") ~ ("v" → value))
        }
        content.attributes must === (expected)
      }
    }

    "GET v1/categories/:context/:formId/shadow" - {
      "returns shadow" in new Fixture {
        val keys = categoriesApi(category.form.id)
          .shadow()
          .as[CategoryShadowResponse]
          .attributes
          .asInstanceOf[JObject]
          .values
          .keys

        keys must contain only (testAttributes.obj.map { case (key, _) ⇒ key }: _*)
      }
    }

    trait Fixture extends StoreAdmin_Seed {
      val testAttributes: JObject       = ("attr1" → "val1")
      val testAttributesMod: JObject    = ("attr1" → "val1") ~ ("attr1" → "val2")
      val testShadowAttributes: JObject = ("attr1" → ("type" → "string"))

      val category = CategoryManager
        .createCategory(
          storeAdmin,
          CreateFullCategory(CreateCategoryForm(testAttributes), CreateCategoryShadow(testShadowAttributes)),
          ctx.name)
        .gimme
    }
  }
}
