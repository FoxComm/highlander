import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ObjectFailures.ObjectContextNotFound
import models.StoreAdmins
import models.activity.ActivityContext
import models.objects.ObjectContexts
import models.product.SimpleContext
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.JsonDSL._
import payloads.CategoryPayloads._
import responses.CategoryResponses._
import services.category.CategoryManager
import util.IntegrationTestBase
import utils.aliases._
import utils.db._

class CategoryIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "Categories" - {
    "GET v1/categories/:context/:formId" - {
      "returns a full category" in new Fixture {
        val response = GET(s"v1/categories/${context.name}/${category.form.id}")
        response.status must === (StatusCodes.OK)

        val content = response.as[FullCategoryResponse.Root]

        content.form.id must === (category.form.id)
        content.shadow.id must === (category.shadow.id)
      }
    }

    "PATCH v1/categories/:context/:formId" - {
      "returns a full category" in new Fixture {
        val newAttribute      = "attr2"
        val newValue          = JString("val2")
        val updatedAttributes = newAttribute → newValue

        val response = PATCH(s"v1/categories/${context.name}/${category.form.id}",
                             UpdateFullCategory(UpdateCategoryForm(updatedAttributes),
                                                UpdateCategoryShadow(updatedAttributes)))

        response.status must === (StatusCodes.OK)

        val content = response.as[FullCategoryResponse.Root]

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

        val response = POST(s"v1/categories/default",
                            CreateFullCategory(CreateCategoryForm(updatedAttributes),
                                               CreateCategoryShadow(updatedAttributes)))

        response.status must === (StatusCodes.OK)

        val content = response.as[FullCategoryResponse.Root]

        val formValues: List[Json] = content.form.attributes.asInstanceOf[JObject].children
        val shadowKeys: Iterable[String] =
          content.shadow.attributes.asInstanceOf[JObject].values.keys

        formValues must contain only newValue
        shadowKeys must contain(newAttribute)
      }
    }

    "GET v1/categories/:formId/form" - {
      "return form" in new Fixture {
        val response = GET(s"v1/categories/${category.form.id}/form")
        response.status must === (StatusCodes.OK)

        val content = response.as[CategoryFormResponse.Root]

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
        val response = GET(s"v1/categories/${context.name}/${category.form.id}/baked")
        response.status must === (StatusCodes.OK)

        val content = response.as[IlluminatedCategoryResponse.Root]

        val expected: JObject = testAttributes.map {
          case (key, value) ⇒ key → JObject(List("v" → value))
        }
        content.attributes must === (expected)
      }
    }

    "GET v1/categories/:context/:formId/shadow" - {
      "returns shadow" in new Fixture {
        val response = GET(s"v1/categories/${context.name}/${category.form.id}/shadow")
        response.status must === (StatusCodes.OK)

        val keys =
          response.as[CategoryShadowResponse.Root].attributes.asInstanceOf[JObject].values.keys

        keys must contain only (testAttributes.map { case (key, _) ⇒ key }: _*)
      }
    }

    trait Fixture {
      implicit val ac: AC = ActivityContext(0, "", "")

      val testAttributes = List("attr1" → JString("val1"))

      val (storeAdmin, context) = (for {
        storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
        context ← * <~ ObjectContexts
                   .filterByName(SimpleContext.default)
                   .one
                   .mustFindOr(ObjectContextNotFound(SimpleContext.default))
      } yield (storeAdmin, context)).gimme

      val category = CategoryManager
        .createCategory(storeAdmin,
                        CreateFullCategory(CreateCategoryForm(testAttributes),
                                           CreateCategoryShadow(testAttributes)),
                        context.name)
        .futureValue
        .rightVal
    }
  }
}
