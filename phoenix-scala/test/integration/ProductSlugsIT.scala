import cats.implicits._
import failures.ProductFailures
import failures.ProductFailures.SlugDuplicates
import payloads.ProductPayloads.UpdateProductPayload
import responses.ProductResponses.ProductResponse.Root
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api._
import testutils.fixtures.api.products.{InvariantProductPayloadBuilder ⇒ PayloadBuilder}
import utils.MockedApis

class ProductSlugsIT
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with ApiFixtures
    with MockedApis {

  "GET v1/products/:context" - {

    "queries product by slug" in {
      val slug = "simple-product"

      val createPayload = PayloadBuilder().createPayload
      val product       = productsApi.create(createPayload).as[Root]

      productsApi(product.id)
        .update(UpdateProductPayload(createPayload.attributes, Some(slug), None, None))
        .mustBeOk()

      productsApi(slug).get().as[Root].id must === (product.id)
    }

    "queries product by slug ignoring case" in {
      val slug = "Simple-Product"

      val product =
        productsApi.create(PayloadBuilder(slug = slug.toLowerCase.some).createPayload).as[Root]

      productsApi(slug).get().as[Root].id must === (product.id)
    }
  }

  "POST v1/products/:context" - {

    "Creates a product with slug" - {
      "successfully" in {
        val possibleSlugs = List("simple-product", "1-Product", "p", "111something")

        possibleSlugs.foreach { slug ⇒
          val createResponse =
            productsApi.create(PayloadBuilder(slug = slug.some).createPayload).as[Root]
          createResponse.slug must === (slug.toLowerCase)

          val getResponse = productsApi(slug).get().as[Root]
          getResponse.slug must === (slug.toLowerCase)
          getResponse.id must === (createResponse.id)
        }
      }

      "generates slug if it is empty" in {
        val slug = ""

        val createResponse =
          productsApi.create(PayloadBuilder(slug = slug.some).createPayload).as[Root]
        createResponse.slug must not be empty

        val getResponse = productsApi(createResponse.slug).get().as[Root]
        getResponse.slug must === (createResponse.slug)
        getResponse.id must === (createResponse.id)
      }

      "generated slug is unique" in {
        val slugs = (1 to 2).map { _ ⇒
          productsApi.create(PayloadBuilder(slug = "".some).createPayload).as[Root].slug
        }

        slugs.forall(!_.isEmpty) must === (true)
        slugs.distinct.size must === (slugs.size)
      }
    }

    "Returns error if" - {
      "slug is invalid" in {
        val invalidSlugValues = Seq("1", "-1", "+1", "-", "_-")
        invalidSlugValues.foreach { slug ⇒
          productsApi
            .create(PayloadBuilder(slug = slug.some).createPayload)
            .mustFailWith400(ProductFailures.SlugShouldHaveLetters(slug))
        }
      }

      "slug is duplicated" in {
        val slug    = "simple-product"
        val payload = PayloadBuilder(slug = slug.some).createPayload

        productsApi.create(payload).mustBeOk()
        productsApi.create(payload).mustFailWith400(SlugDuplicates(slug))
      }

      "slugs differ only by case" in {
        val slug = "simple-product"
        productsApi.create(PayloadBuilder(slug = slug.some).createPayload).mustBeOk()

        val duplicatedSlug: String = slug.toUpperCase()
        productsApi
          .create(PayloadBuilder(slug = duplicatedSlug.some).createPayload)
          .mustFailWith400(SlugDuplicates(duplicatedSlug))
      }
    }
  }

  "PATCH v1/products/:context/:id" - {

    "Updates slug successfully" in {
      val slug = "simple-product"

      val payloadBuilder = PayloadBuilder()
      val product        = productsApi.create(payloadBuilder.createPayload).as[Root]

      productsApi(product.id)
        .update(payloadBuilder.updatePayload(slug = slug.some))
        .as[Root]
        .slug must === (slug)
    }

    "Updates uppercase slug successfully" in {
      val slug = "Simple-Product"

      val payloadBuilder = PayloadBuilder()
      val product        = productsApi.create(payloadBuilder.createPayload).as[Root]

      productsApi(product.id)
        .update(payloadBuilder.updatePayload(slug = slug.some))
        .as[Root]
        .slug must === (slug.toLowerCase)
    }

    "Returns error if" - {

      "slug is invalid" in {
        val payloadBuilder = PayloadBuilder()

        val product = productsApi.create(payloadBuilder.createPayload).as[Root]

        val invalidSlugValues = Seq("1", "-1", "+1", "_", "-")
        invalidSlugValues.foreach { slug ⇒
          productsApi(product.id)
            .update(payloadBuilder.updatePayload(slug = slug.some))
            .mustFailWith400(ProductFailures.SlugShouldHaveLetters(slug))
        }
      }

      "slug is duplicated" in {
        val slug = "simple-product"

        productsApi.create(PayloadBuilder(slug = slug.some).createPayload).mustBeOk()

        val product2PayloadBuilder = PayloadBuilder()

        val product2 = productsApi.create(product2PayloadBuilder.createPayload).as[Root]

        productsApi(product2.id)
          .update(product2PayloadBuilder.updatePayload(slug = slug.some))
          .mustFailWith400(SlugDuplicates(slug))
      }
    }
  }
}
