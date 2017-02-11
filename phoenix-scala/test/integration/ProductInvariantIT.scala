import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import failures.ArchiveFailures._
import failures.ObjectFailures.ObjectContextNotFound
import failures.ProductFailures._
import faker.Lorem
import models.objects._
import models.product._
import org.json4s.JsonDSL._
import org.json4s._
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.ProductPayloads._
import payloads.ProductVariantPayloads.ProductVariantPayload
import responses.ProductResponses.ProductResponse.Root
import testutils.PayloadHelpers._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import testutils.fixtures.api.products.{InvariantProductPayloadBuilder ⇒ PayloadBuilder, _}
import utils.aliases._
import utils.time.RichInstant

// Tests for products with single variant and no options
class ProductInvariantIT
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures
    with TaxonomySeeds // rewrite taxon test and remove this
    with ApiFixtures
    with ApiFixtureHelpers {

  "GET /v1/products/:context" - {
    "Gets an associated variant after creating a product with a variant" in new ProductVariant_ApiFixture {
      productsApi(product.id).get().as[Root].variants.onlyElement.attributes.code must === (
          productVariantCode)
    }

    "returns assigned taxonomies" in new StoreAdmin_Seed with ProductVariant_ApiFixture
    with FlatTaxons_Baked {
      val taxonId = taxons.head.formId

      taxonApis(taxonId).assignProduct(product.id).mustBeOk()
    }
  }

  "POST v1/products/:context" - {
    "Creates a product with" - {
      "a new variant successfully" in new ProductVariant_ApiFixture {
        productVariant.attributes.code must === (productVariantCode)
        productVariant.skuId must be > 0
      }

      "an existing variant successfully" in new ProductVariant_ApiFixture {
        val reusedVariantPayload =
          PayloadBuilder().createPayload.copy(variants = Seq(payloadBuilder.variantPayload))

        val product2 = productsApi.create(reusedVariantPayload).as[Root]
        product2.variants.onlyElement.attributes.code must === (
            product.variants.onlyElement.attributes.code)
      }

      "an existing, but modified variant successfully" in new ProductVariant_ApiFixture {
        val variantCode = payloadBuilder.productVariantCode
        val newPrice    = 7999

        // Reuse variant code from product 1, but change price and add album
        val reusedVariantPayload =
          buildVariantPayload(code = variantCode, price = newPrice, albums = someAlbums)

        // Create new product reusing same variant
        val createPayload2 =
          PayloadBuilder().createPayload.copy(variants = Seq(reusedVariantPayload))

        val reusedVariant = productsApi.create(createPayload2).as[Root].variants.onlyElement
        reusedVariant.attributes.code must === (variantCode)
        reusedVariant.attributes.salePrice must === (newPrice)
        reusedVariant.attributes.retailPrice must === (newPrice)
        reusedVariant.albums must have size 1
      }

      "a product with album successfully" in {
        val createResponse =
          productsApi.create(PayloadBuilder(albums = someAlbums).createPayload).as[Root]
        createResponse.albums.onlyElement.images.onlyElement.src must === (imageSrc)

        val getResponse = productsApi(createResponse.id).get().as[Root]
        getResponse.albums.onlyElement.images.onlyElement.src must === (imageSrc)
      }

      "a variant with an album successfully" in {
        val payloadBuilder = PayloadBuilder()
        val createPayload = payloadBuilder.createPayload.copy(
            variants = Seq(payloadBuilder.variantPayload.copy(albums = someAlbums)))

        val product = productsApi.create(createPayload).as[Root]

        productsApi(product.id)
          .get()
          .as[Root]
          .variants
          .onlyElement
          .albums
          .onlyElement
          .images
          .onlyElement
          .src must === (imageSrc)
      }
    }

    "Returns an error if" - {
      "no variant is added" in {
        val payload = PayloadBuilder().createPayload.copy(variants = Seq.empty)
        productsApi.create(payload).mustFailWithMessage("Product variants must not be empty")
      }

      "trying to create a product and variant with no code" in {
        val createPayload = PayloadBuilder().createPayload
          .copy(variants = Seq(ProductVariantPayload(attributes = Map.empty)))

        productsApi.create(createPayload).mustFailWithMessage("SKU code not found in payload")
      }

      "trying to create a product and variant with empty code" in new Schemas_Seed {
        val createPayload =
          PayloadBuilder().createPayload.copy(variants = Seq(buildVariantPayload(code = "")))

        val errorMessage =
          """Object product-variant with id=3 doesn't pass validation: $.code: must be at least 1 characters long"""
        productsApi.create(createPayload).mustFailWithMessage(errorMessage)
      }

      "trying to create a product with string price" in new Schemas_Seed {
        val failPriceAttr: Json = tv(("currency" → "USD") ~ ("value" → "666"), "price")

        // Stuff wrong price attr instead of a good one
        val createPayload = {
          val payloadBuilder = PayloadBuilder()
          val newVariantAttrs =
            payloadBuilder.variantPayload.attributes.updated("salePrice", failPriceAttr)
          val variantPayload = ProductVariantPayload(newVariantAttrs)
          payloadBuilder.createPayload.copy(variants = Seq(variantPayload))
        }

        val createResponse = productsApi.create(createPayload)

        createResponse.mustHaveStatus(StatusCodes.BadRequest)
        val errorPattern =
          "Object product-variant with id=\\d+ doesn't pass validation: \\$.salePrice.value: string found, number expected"
        createResponse.error must fullyMatch regex errorPattern.r
      }

      "there is more than one variant and no options" in {
        val createPayload = PayloadBuilder().createPayload
          .copy(variants = Seq(buildVariantPayload(), buildVariantPayload()))

        productsApi
          .create(createPayload)
          .mustFailWithMessage("number of product variants got 2, expected 1 or less")
      }

      "trying to create a product with archived variant" in new ProductVariant_ApiFixture {
        productVariantsApi(productVariant.id).archive().mustBeOk()

        productsApi
          .create(payloadBuilder.createPayload.copy(slug = ""))
          .mustFailWith400(LinkArchivedVariantFailure(Product, 2, productVariantCode))
      }
    }
  }

  "PATCH v1/products/:context/:id" - {

    "Keeps everything if update payload is empty" in new ProductVariant_ApiFixture {
      val updatePayload = UpdateProductPayload(attributes = Map.empty,
                                               // FIXME @anna Setting variants to `None` must not remove them from product
                                               variants =
                                                 payloadBuilder.createPayload.variants.some,
                                               options = None,
                                               albums = None)

      productsApi(product.id).update(updatePayload).as[Root] must === (product)
    }

    "Doesn't complain if you do update w/o any changes" in new ProductVariant_ApiFixture {
      val cartRef = api_newCustomerCart(api_newCustomer().id).referenceNumber

      cartsApi(cartRef).lineItems
        .add(Seq(UpdateLineItemsPayload(product.variants.onlyElement.id, 1)))
        .mustBeOk()

      productsApi(product.id).update(payloadBuilder.updatePayload()).mustBeOk()
    }

    "Updates a variant with an album successfully" in new ProductVariant_ApiFixture {
      val updVariantPayload = payloadBuilder.variantPayload.copy(albums = someAlbums)
      val updatePayload     = payloadBuilder.updatePayload(variants = Seq(updVariantPayload).some)

      productsApi(product.id)
        .update(updatePayload)
        .as[Root]
        .variants
        .onlyElement
        .albums
        .onlyElement
        .images
        .onlyElement
        .src must === (imageSrc)
    }

    "Updates an album on a product" in new ProductVariant_ApiFixture {
      productsApi(product.id)
        .update(payloadBuilder.updatePayload(albums = someAlbums))
        .as[Root]
        .albums
        .onlyElement
        .images
        .onlyElement
        .src must === (imageSrc)
    }

    "Updates and replaces a variant on the product" in new ProductVariant_ApiFixture {
      val newProductName  = Lorem.sentence(1)
      val newProductDescr = Lorem.sentence(5)
      val newVariantCode  = Lorem.letterify("??????")

      val updatedProduct = {
        val newProductAttrs   = productAttrs(name = newProductName, description = newProductDescr)
        val newVariantPayload = buildVariantPayload(newVariantCode, payloadBuilder.price)

        productsApi(product.id)
          .update(payloadBuilder.updatePayload(attributes = newProductAttrs.some,
                                               variants = Seq(newVariantPayload).some))
          .as[Root]
      }
      updatedProduct.options mustBe empty
      updatedProduct.variants.onlyElement.attributes.code must === (newVariantCode)
      updatedProduct.attributes.getString("name") must === (newProductName)
      updatedProduct.attributes.getString("description") must === (newProductDescr)
    }

    "Removes variants from product" in new ProductVariant_ApiFixture {
      productsApi(product.id)
        .update(payloadBuilder.updatePayload(variants = Seq.empty.some))
        .as[Root]
        .variants mustBe empty
    }

    "Multiple calls with same params do not duplicate variants" in new ProductVariant_ApiFixture {
      // Update with same payload
      productsApi(product.id)
        .update(payloadBuilder.updatePayload())
        .as[Root]
        .variants must have size 1
      // And once more
      productsApi(product.id)
        .update(payloadBuilder.updatePayload())
        .as[Root]
        .variants must have size 1
    }

    "Updates attributes on a product successfully" in new ProductVariant_ApiFixture {
      val newName = Lorem.sentence(1)

      val response = productsApi(product.id)
        .update(payloadBuilder.updatePayload(attributes = productAttrs(name = newName).some))
        .as[Root]
      response.variants must have size 1
      response.attributes.getString("name") must === (newName)
    }

    "Returns an error if" - {
      "trying to update a product with archived variant" in new ProductVariant_ApiFixture {
        productVariantsApi(product.variants.onlyElement.id).archive().mustBeOk()

        productsApi(product.id)
          .update(payloadBuilder.updatePayload())
          .mustFailWith400(
              LinkArchivedVariantFailure(Product, product.id, payloadBuilder.productVariantCode))
      }

      "trying to unassociate a Variant that is in cart" in new ProductVariant_ApiFixture {
        val cartRef = api_newCustomerCart(api_newCustomer().id).referenceNumber

        cartsApi(cartRef).lineItems
          .add(Seq(UpdateLineItemsPayload(product.variants.onlyElement.id, 1)))
          .mustBeOk()

        productsApi(product.id)
          .update(payloadBuilder.updatePayload(variants = Seq.empty.some))
          .mustFailWith400(VariantIsPresentInCarts(payloadBuilder.productVariantCode))
      }
    }
  }

  "DELETE v1/products/:context/:id" - {
    "Archives product successfully" in new ProductVariant_ApiFixture {
      val result = productsApi(product.id).archive().as[Root]
      withClue(result.archivedAt.value → Instant.now) {
        result.archivedAt.value.isBeforeNow must === (true)
      }
    }

    "Archived product must be inactive" in new ProductVariant_ApiFixture {
      val attributes = productsApi(product.id).archive().as[Root].attributes

      attributes.getOpt[Instant]("activeTo") must not be 'defined
      attributes.getOpt[Instant]("activeFrom") must not be 'defined
    }

    "Returns error if product is present in carts" in new ProductVariant_ApiFixture {
      val cartRef = api_newCustomerCart(api_newCustomer().id).referenceNumber
      cartsApi(cartRef).lineItems.add(Seq(UpdateLineItemsPayload(productVariant.id, 1))).mustBeOk()

      productsApi(product.id).archive().mustFailWith400(ProductIsPresentInCarts(product.id))
    }

    "Variants must be unlinked" in new ProductVariant_ApiFixture {
      productsApi(product.id).archive().as[Root].variants mustBe empty
    }

    "Albums must be unlinked" in new ProductVariant_ApiFixture {
      productsApi(product.id).archive().as[Root].albums mustBe empty
    }

    "Responds with NOT FOUND when wrong product is requested" in {
      productsApi(666).archive().mustFailWith404(ProductFormNotFoundForContext(666, ctx.id))
    }

    "Responds with NOT FOUND when wrong context is requested" in new ProductVariant_ApiFixture {
      productsApi(product.id)(ObjectContext(name = "donkeyContext", attributes = JNothing))
        .archive()
        .mustFailWith404(ObjectContextNotFound("donkeyContext"))
    }
  }
}
