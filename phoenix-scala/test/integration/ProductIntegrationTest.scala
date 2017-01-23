import java.time.Instant

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

import cats.implicits._
import failures.ArchiveFailures._
import failures.ObjectFailures.ObjectContextNotFound
import failures.ProductFailures
import failures.ProductFailures._
import models.account.Scope
import models.account.User
import models.inventory._
import models.objects._
import models.product._
import org.json4s.JsonDSL._
import org.json4s._
import payloads.ImagePayloads._
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.CreateCart
import payloads.ProductPayloads._
import payloads.ProductVariantPayloads.ProductVariantPayload
import payloads.ProductOptionPayloads.{ProductOptionPayload, ProductOptionValuePayload}
import responses.ProductResponses.ProductResponse
import responses.ProductResponses.ProductResponse.Root
import responses.cord.CartResponse
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtures
import utils.JsonFormatters
import utils.Money.Currency
import utils.aliases._
import utils.db._
import utils.time.RichInstant

object ProductTestExtensions {

  implicit class RichAttributes(val attributes: Json) extends AnyVal {
    def code: String = {
      implicit val formats = JsonFormatters.phoenixFormats
      (attributes \ "code" \ "v").extract[String]
    }
  }
}

// To whoever will be rewriting this: possible room for regressions includes incorrect handling of SKUs or variants
// added to carts. I can't put some guards in here against that because I'll have to rewrite almost all tests in this
// file. If you add a new test, consider adding one where gets added to cart before update/archive just to make sure.
// -- Anna

class ProductIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures
    with ApiFixtures
    with TaxonomySeeds {
  import ProductTestExtensions._

  "GET v1/products/:context" - {
    "returns assigned taxonomies" in new StoreAdmin_Seed with ProductVariant_ApiFixture
    with FlatTaxons_Baked {
      taxonApi(taxons.head.formId).assignProduct(product.id).mustBeOk()
      val updatedProduct = productsApi(product.id).get().as[ProductResponse.Root]
      updatedProduct.taxons.map(_.taxon.id) must contain(taxons.head.formId)
    }

    "queries product by slug" in new ProductVariant_ApiFixture {
      val slug          = "simple-product"
      val simpleProduct = Products.mustFindById404(product.id).gimme

      val updated = simpleProduct.copy(slug = slug)

      productsApi(product.id)
        .update(
            UpdateProductPayload(productPayload.attributes,
                                 slug = Some(slug),
                                 variants = None,
                                 options = None))
        .mustBeOk()

      productsApi(slug).get().as[ProductResponse.Root].id must === (updated.formId)
    }

    "queries product by slug ignoring case" in new ProductVariant_ApiFixture {
      val slug          = "Simple-Product"
      val simpleProduct = Products.mustFindById404(product.id).gimme
      val updated       = simpleProduct.copy(slug = slug.toLowerCase)

      Products.update(simpleProduct, updated).gimme

      productsApi(slug).get().as[ProductResponse.Root].id must === (updated.formId)
    }
  }

  "POST v1/products/:context" - {

    "Creates a product with" - {
      val skuName = "SKU-NEW-TEST"

      "slug successfully" in new Fixture {
        val possibleSlug = List("simple-product", "1-Product", "p", "111something")
        for (slug ← possibleSlug) {
          val slugClue = s"slug: $slug"

          val productResponse = productsApi.create(productPayload.copy(slug = slug)).as[Root]
          productResponse.slug must === (slug.toLowerCase).withClue(slugClue)

          val getProductResponse = productsApi(slug).get().as[Root]
          getProductResponse.slug must === (slug.toLowerCase).withClue(slugClue)
          getProductResponse.id must === (productResponse.id).withClue(slugClue)
        }
      }

      "generates slug if it is empty" in new Fixture {
        val slug = ""

        val productResponse = productsApi.create(productPayload.copy(slug = slug)).as[Root]
        productResponse.slug.isEmpty must === (false)

        val generatedSlug      = productResponse.slug
        val getProductResponse = productsApi(generatedSlug).get().as[Root]
        getProductResponse.slug must === (generatedSlug)
        getProductResponse.id must === (productResponse.id)
      }

      "generated slug is unique" in new Fixture {
        val productResponses = for (_ ← 1 to 2)
          yield productsApi.create(productPayload.copy(slug = "")).as[Root]

        val slugs = productResponses.map(_.slug)

        slugs.forall(!_.isEmpty) must === (true)
        slugs.distinct.size must === (slugs.size)
      }

      "a new variant successfully" in new Fixture {
        val productResponse = productsApi.create(productPayload).as[Root]
        productResponse.variants.length must === (1)
        private val variant = productResponse.variants.head
        variant.attributes.code must === (skuName)
        variant.middlewarehouseSkuId must be > 0
      }

      "a new variant with option successfully" in new Fixture {
        val valuePayload = Seq(
            ProductOptionValuePayload(skuCodes = Seq(skuName), swatch = None, name = Some("Test")))
        val variantPayload =
          Seq(ProductOptionPayload(attributes = Map("test" → "Test"), values = Some(valuePayload)))

        val productResponse =
          productsApi.create(productPayload.copy(options = Some(variantPayload))).as[Root]

        productResponse.variants.length must === (1)
        productResponse.variants.head.attributes.code must === (skuName)

        productResponse.options.size must === (1)
        val variant = productResponse.options.head
        variant.values.size must === (1)
        variant.values.head.skuCodes must contain only skuName
      }

      "Gets an associated variant after creating a product with a variant" in new Fixture {
        val redSkuPayload = makeVariantPayload("SKU-RED-SMALL", skuAttrMap, None)
        val payload       = productPayload.copy(variants = Seq(redSkuPayload))

        val productResponse    = productsApi.create(payload).as[Root]
        val getProductResponse = productsApi(productResponse.id).get().as[Root]
        getProductResponse.variants.length must === (1)

        val getFirstSku :: Nil = getProductResponse.variants
        val getCode            = getFirstSku.attributes \ "code" \ "v"
        getCode.extract[String] must === ("SKU-RED-SMALL")
      }

      "an existing variant successfully" in new Fixture {
        val redSkuPayload = makeVariantPayload(skuRedSmallCode, skuAttrMap, None)
        val payload       = productPayload.copy(variants = Seq(redSkuPayload))

        val productResponse = productsApi.create(payload).as[Root]

        productResponse.variants.length must === (1)
        productResponse.variants.head.attributes.code must === (skuRedSmallCode)
      }

      "an existing variant with options successfully" in new VariantFixture {
        val payload         = productPayload.copy(options = Some(Seq(justColorVariantPayload)))
        val productResponse = productsApi.create(payload).as[Root]

        productResponse.options.length must === (1)
        productResponse.options.head.values.length must === (2)

        val skuCodes        = productResponse.variants.map(s ⇒ s.attributes.code)
        val variantSkuCodes = productResponse.options.head.values.flatMap(v ⇒ v.skuCodes)
        val expectedSkus    = justColorVariantPayload.values.getOrElse(Seq.empty).flatMap(_.skuCodes)

        skuCodes must contain only (expectedSkus: _*)
        variantSkuCodes must contain only (expectedSkus: _*)
      }

      "an existing, but modified variant successfully" in new Fixture {
        val redPriceJson  = ("t" → "price") ~ ("v" → (("currency" → "USD") ~ ("value" → 7999)))
        val redSkuAttrMap = Map("salePrice" → redPriceJson)
        val src           = "http://lorempixel/test.png"
        val imagePayload  = ImagePayload(src = src)
        val albumPayload  = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

        val redSkuPayload =
          makeVariantPayload(skuRedLargeCode, redSkuAttrMap, Seq(albumPayload).some)
        val payload = productPayload.copy(variants = Seq(redSkuPayload))

        val productResponse = productsApi.create(payload).as[Root]

        productResponse.variants.length must === (1)
        productResponse.variants.head.attributes.code must === (skuRedLargeCode)
      }

      "empty productOption successfully" in new Fixture {
        val redSkuPayload = makeVariantPayload(skuRedSmallCode, skuAttrMap, None)
        val values =
          Seq(ProductOptionValuePayload(name = Some("value"), swatch = None, skuCodes = Seq.empty))
        val variantPayload =
          Seq(ProductOptionPayload(attributes = Map("t" → "t"), values = Some(values)))
        val payload =
          productPayload.copy(variants = Seq(redSkuPayload), options = Some(variantPayload))

        val response = productsApi.create(payload).as[Root]

        response.options.length must === (1)
        response.options.head.values.length must === (1)
        response.options.head.values.head.skuCodes.length must === (0)
        response.variants.length must === (0)
      }

      "an album successfully" in new Fixture {
        val src          = "http://lorempixel/test.png"
        val imagePayload = ImagePayload(src = src)
        val albumPayload = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

        val productResponse =
          productsApi.create(productPayload.copy(albums = Seq(albumPayload).some)).as[Root]
        productResponse.albums.length must === (1)
        productResponse.albums.head.images.length must === (1)
        productResponse.albums.head.images.head.src must === (src)

        val getProductResponse = productsApi(productResponse.id).get().as[Root]
        getProductResponse.albums.length must === (1)
        getProductResponse.albums.head.images.length must === (1)
        getProductResponse.albums.head.images.head.src must === (src)
      }

      "a variant with an album successfully" in new Fixture {
        val src          = "http://lorempixel/test.png"
        val imagePayload = ImagePayload(src = src)
        val albumPayload = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

        val newSkuPayload     = productPayload.variants.head.copy(albums = Seq(albumPayload).some)
        val newProductPayload = productPayload.copy(variants = Seq(newSkuPayload))

        val productResponse = productsApi.create(newProductPayload).as[Root]
        productResponse.variants.length must === (1)
        productResponse.variants.head.albums.length must === (1)

        val getProductResponse = productsApi(productResponse.id).get().as[Root]
        getProductResponse.variants.length must === (1)

        val album :: Nil = getProductResponse.variants.head.albums
        album.images.length must === (1)
        album.images.head.src must === (src)
      }
    }

    "Throws an error if" - {
      "no variant is added" in new Fixture {
        val payload = productPayload.copy(variants = Seq.empty)
        productsApi.create(payload).mustFailWithMessage("Product variants must not be empty")
      }

      "no variant exists for product options" in new Fixture {
        val values = Seq(
            ProductOptionValuePayload(name = Some("name"),
                                      swatch = None,
                                      skuCodes = Seq("SKU-TEST1", "SKU-TEST2")))
        val variantPayload =
          Seq(ProductOptionPayload(attributes = Map("t" → "t"), values = Some(values)))
        val payload = productPayload.copy(variants = Seq.empty, options = Some(variantPayload))

        productsApi.create(payload).mustFailWithMessage("Product variants must not be empty")
      }

      "there is more than one variant and no variants" in new Fixture {
        val sku1    = makeVariantPayload("SKU-TEST-NUM1", attrMap, None)
        val sku2    = makeVariantPayload("SKU-TEST-NUM2", attrMap, None)
        val payload = productPayload.copy(variants = Seq(sku1, sku2), options = Some(Seq.empty))

        productsApi
          .create(payload)
          .mustFailWithMessage("number of product variants got 2, expected 1 or less")
      }

      "trying to create a product and variant with no code" in new Fixture {
        val newProductPayload = productPayload.copy(
            variants = Seq(ProductVariantPayload(attributes = skuAttrMap, albums = None)))

        productsApi.create(newProductPayload).mustFailWithMessage("SKU code not found in payload")
      }

      "trying to create a product and variant with empty code" in new Fixture {
        val newProductPayload =
          productPayload.copy(variants = Seq(makeVariantPayload("", skuAttrMap, None)))

        productsApi
          .create(newProductPayload)
          .mustFailWithMessage(
              """Object product-variant with id=13 doesn't pass validation: $.code: must be at least 1 characters long""".stripMargin)
      }

      "trying to create a product with archived variant" in new ArchivedSkuFixture {
        productsApi
          .create(archivedSkuProductPayload)
          .mustFailWith400(LinkArchivedVariantFailure(Product, 2, archivedSkuCode))
      }

      "trying to create a product with string price" in new Fixture {
        val price: Json = ("t" → "price") ~ ("v" → (("currency"
                        → "USD") ~ ("value" → "1000")))
        val skuAttributes: Map[String, Json] = skuPayload.attributes + ("salePrice" → price)
        val productToCreate =
          productPayload.copy(variants = Seq(skuPayload.copy(attributes = skuAttributes)))
        val createResponse = productsApi.create(productToCreate)

        createResponse.mustHaveStatus(StatusCodes.BadRequest)
        val errorPattern =
          "Object product-variant with id=\\d+ doesn't pass validation: \\$.salePrice.value: string found, number expected"
        createResponse.error must fullyMatch regex errorPattern.r
      }

      "slug is invalid" in new Fixture {
        val invalidSlugValues = Seq("1", "-1", "+1", "-", "_-")
        for (slug ← invalidSlugValues) {
          productsApi
            .create(productPayload.copy(slug = slug))
            .mustFailWith400(ProductFailures.SlugShouldHaveLetters(slug))
            .withClue(s" slug = $slug")
        }
      }

      "slug is duplicated" in new Fixture {
        val slug    = "simple-product"
        val payload = productPayload.copy(slug = slug)
        productsApi.create(payload).mustBeOk()

        productsApi.create(payload).mustFailWith400(SlugDuplicates(slug))
      }

      "slugs differ only by case" in new Fixture {
        val slug = "simple-product"
        productsApi.create(productPayload.copy(slug = slug)).mustBeOk()
        val duplicatedSlug: String = slug.toUpperCase()
        productsApi
          .create(productPayload.copy(slug = duplicatedSlug))
          .mustFailWith400(SlugDuplicates(duplicatedSlug))
      }
    }

    "Creates a product then requests is successfully" in new Fixture {
      val productId = productsApi.create(productPayload).as[Root].id

      val response = productsApi(productId).get().as[Root]
      response.variants.length must === (1)
      response.variants.head.attributes.code must === ("SKU-NEW-TEST")
    }
  }

  "PATCH v1/products/:context/:id" - {

    "Doesn't complain if you do update w/o any changes" in new Customer_Seed with Fixture {
      private val cartRef =
        cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

      cartsApi(cartRef).lineItems.add(allSkus.map(sku ⇒ UpdateLineItemsPayload(sku, 1))).mustBeOk()

      productsApi(product.formId)
        .update(
            UpdateProductPayload(
                attributes = attrMap,
                variants = allSkus.map(sku ⇒ makeVariantPayload(sku, skuAttrMap, None)).some,
                albums = None,
                options = None))
        .mustBeOk()
    }

    "Updates slug successfully" in new Fixture {
      val slug = "simple-product"

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         slug = slug.some,
                                         variants = Some(Seq(skuPayload)),
                                         options = None,
                                         albums = None)

      productsApi(product.formId).update(payload).as[Root].slug must === (slug)
    }

    "Updates uppercase slug successfully" in new Fixture {
      val slug = "Simple-Product"

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         slug = slug.some,
                                         variants = Some(Seq(skuPayload)),
                                         options = None,
                                         albums = None)

      productsApi(product.formId).update(payload).as[Root].slug must === (slug.toLowerCase)
    }

    "Updates the variants on a product successfully" in new Fixture {
      val payload = UpdateProductPayload(attributes = Map.empty,
                                         variants = Some(Seq(skuPayload)),
                                         options = None,
                                         albums = None)

      val response = productsApi(product.formId).update(payload).as[Root]
      response.variants.length must === (4)
      response.options.length must === (2)

      val description = response.attributes \ "description" \ "v"
      description.extract[String] must === ("Test product description")
    }

    "Updates a variant with an album successfully" in new Fixture with Product_Raw {
      val src           = "http://lorempixel/test.png"
      val imagePayload  = ImagePayload(src = src)
      val albumPayload  = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)
      val albumsPayload = Seq(albumPayload).some

      val updateSkuPayload = makeVariantPayload("SKU-UPDATE-TEST", skuAttrMap, albumsPayload)
      val newAttrMap       = Map("name" → (("t" → "string") ~ ("v" → "Some new product name")))
      val payload = UpdateProductPayload(attributes = newAttrMap,
                                         variants = Some(Seq(updateSkuPayload)),
                                         albums = None,
                                         options = Some(Seq.empty))

      val response = productsApi(simpleProduct.formId).update(payload).as[Root]
      response.variants.length must === (1)

      val getProductResponse = productsApi(response.id).get().as[Root]
      getProductResponse.variants.length must === (1)

      val sku :: Nil = getProductResponse.variants
      sku.albums.length must === (1)

      val album :: Nil = sku.albums
      album.images.length must === (1)
      album.images.head.src must === (src)
    }

    "Updates an album on a product" in new Fixture {
      val src          = "http://lorempixel/test.png"
      val imagePayload = ImagePayload(src = src)
      val albumPayload = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

      val newSkuPayload = productPayload.variants.head.copy(albums = Seq(albumPayload).some)

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         variants = Seq(newSkuPayload).some,
                                         options = Seq.empty.some,
                                         albums = Seq(albumPayload).some)

      val productResponse = productsApi(product.formId).update(payload).as[Root]
      productResponse.albums.length must === (1)

      val getProductResponse = productsApi(productResponse.id).get().as[Root]
      getProductResponse.albums.length must === (1)

      val album :: Nil = getProductResponse.albums
      album.images.length must === (1)
      album.images.head.src must === (src)

    }

    "Updates and replaces a variant on the product" in new Fixture with Product_Raw {
      val updateSkuPayload = makeVariantPayload("SKU-UPDATE-TEST", skuAttrMap, None)
      val newAttrMap       = Map("name" → (("t" → "string") ~ ("v" → "Some new product name")))
      val payload = UpdateProductPayload(attributes = newAttrMap,
                                         variants = Some(Seq(updateSkuPayload)),
                                         albums = None,
                                         options = Some(Seq.empty))

      val response = productsApi(simpleProduct.formId).update(payload).as[Root]
      response.variants.length must === (1)
      response.options.length must === (0)

      val skuResponse = response.variants.head
      val code        = skuResponse.attributes \ "code" \ "v"
      code.extract[String] must === ("SKU-UPDATE-TEST")

      val description = response.attributes \ "description" \ "v"
      description.extract[String] must === ("Test product description")

      val name = response.attributes \ "name" \ "v"
      name.extract[String] must === ("Some new product name")
    }

    "Removes variants from product" in new Fixture {
      productsApi(product.formId)
        .update(
            UpdateProductPayload(attributes = attrMap,
                                 variants = Seq.empty.some,
                                 albums = None,
                                 options = Seq.empty.some))
        .as[Root]
        .variants mustBe empty
    }

    "Removes some variants from product" in new RemovingSkusFixture {
      productsApi(product.formId).get.as[Root].variants must have size 4

      val remainingSkus: Seq[String] = productsApi(product.formId)
        .update(twoSkuProductPayload)
        .as[Root]
        .variants
        .map(sku ⇒ (sku.attributes \ "code" \ "v").extract[String])

      remainingSkus must have size 2
      remainingSkus must contain theSameElementsAs Seq(skuRedLargeCode, skuGreenSmallCode)
    }

    "Updates the SKUs on a product if variants are Some(Seq.empty)" in new Fixture {

      ProductVariantLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme
      ProductOptionLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         variants = Some(Seq(skuPayload)),
                                         albums = None,
                                         options = Some(Seq.empty))

      val response = productsApi(product.formId).update(payload).as[Root]
      response.variants.length must === (1)
      response.options.length must === (0)

      val description = response.attributes \ "description" \ "v"
      description.extract[String] must === ("Test product description")
    }

    "Multiple calls with same params create single SKU link" in new Fixture {

      ProductVariantLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme
      ProductOptionLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         variants = Some(Seq(skuPayload)),
                                         options = None,
                                         albums = None)

      var response = productsApi(product.formId).update(payload).as[Root]
      response.variants.length must === (1)

      response = productsApi(product.formId).update(payload).as[Root]
      response.variants.length must === (1)

      ProductVariantLinks.filterLeft(product).gimme.size must === (1)
    }

    "Updates the properties on a product successfully" in new Fixture {
      val newAttrMap = Map("name" → (("t" → "string") ~ ("v" → "Some new product name")))
      val payload = UpdateProductPayload(attributes = newAttrMap,
                                         variants = None,
                                         options = None,
                                         albums = None)

      val response = productsApi(product.formId).update(payload).as[Root]
      response.variants.length must === (4)
      response.options.length must === (2)
    }

    "Updates the variants" - {
      "Remove all variants successfully" in new Fixture {
        val payload = UpdateProductPayload(attributes = Map.empty,
                                           variants = None,
                                           options = Some(Seq()),
                                           albums = None)

        val response = productsApi(product.formId).update(payload).as[Root]
        response.variants.length must === (0)
        response.options.length must === (0)
      }

      "Add new productOption with new SKU successfully" in new VariantFixture {
        private val newSkuCode: ActivityType = "SKU-NEW-TEST"
        val newSkuPayload                    = makeVariantPayload(newSkuCode, skuAttrMap, None)

        val goldValuePayload =
          ProductOptionValuePayload(name = Some("Gold"), swatch = None, skuCodes = Seq(newSkuCode))
        val silverValuePayload =
          ProductOptionValuePayload(name = Some("Silver"), swatch = None, skuCodes = Seq.empty)
        val metalVariantPayload =
          productOptionPayload("Metal", Seq(goldValuePayload, silverValuePayload))

        val payload = UpdateProductPayload(attributes = Map.empty,
                                           variants = Some(Seq(newSkuPayload)),
                                           albums = None,
                                           options =
                                             Some(colorSizeVariants.+:(metalVariantPayload)))

        val response = productsApi(product.formId).update(payload).as[Root]
        response.variants.length must === (5)
        response.options.length must === (3)
        response.variants.map(_.attributes.code) must contain(newSkuCode)
      }
    }

    "Throws an error" - {
      "if updating adds too many SKUs" in new VariantFixture {
        val upPayload = UpdateProductPayload(
            attributes = Map.empty,
            variants = Some(
                Seq(skuPayload,
                    smallRedSkuPayload,
                    smallGreenSkuPayload,
                    largeRedSkuPayload,
                    largeGreenSkuPayload)),
            options = None,
            albums = None
        )

        productsApi(product.formId)
          .update(upPayload)
          .mustFailWithMessage(
              "number of product variants for given options got 5, expected 4 or less")
      }

      "trying to update a product with archived variant" in new ArchivedSkuFixture {
        productsApi(product.formId)
          .update(UpdateProductPayload(attributes = archivedSkuProductPayload.attributes,
                                       variants = archivedSkuProductPayload.variants.some,
                                       albums = None,
                                       options = archivedSkuProductPayload.options))
          .mustFailWith400(LinkArchivedVariantFailure(Product, product.id, archivedSkuCode))
      }

      "trying to unassociate a Variant that is in cart" in new RemovingSkusFixture {
        val cartRefNum =
          cartsApi.create(CreateCart(email = "yax@yax.com".some)).as[CartResponse].referenceNumber

        cartsApi(cartRefNum).lineItems
          .add(Seq(UpdateLineItemsPayload(skuGreenLargeCode, 1)))
          .mustBeOk()

        productsApi(product.formId)
          .update(twoSkuProductPayload)
          .mustFailWith400(VariantIsPresentInCarts(skuGreenLargeCode))
      }

      "slug is invalid" in new Fixture {
        val createdProduct = productsApi.create(productPayload).as[Root]

        val invalidSlugValues = Seq("1", "-1", "+1", "_", "-")
        for (slug ← invalidSlugValues) {
          productsApi(createdProduct.id)
            .update(UpdateProductPayload(attributes = productPayload.attributes,
                                         slug = Some(slug),
                                         variants = None,
                                         options = None))
            .mustFailWith400(ProductFailures.SlugShouldHaveLetters(slug))
            .withClue(s" slug = $slug")
        }
      }

      "slug is duplicated" in new Fixture {
        val slug = "simple-product"

        productsApi.create(productPayload.copy(slug = slug)).mustBeOk()
        val product2 = productsApi.create(productPayload).as[Root]

        private val updateResponse: HttpResponse = productsApi(product2.id).update(
            UpdateProductPayload(attributes = productPayload.attributes,
                                 slug = Some(slug),
                                 variants = None,
                                 options = None))

        updateResponse.mustFailWith400(SlugDuplicates(slug))
      }
    }
  }

  "DELETE v1/products/:context/:id" - {
    "Archives product successfully" in new Fixture {
      val result = productsApi(product.formId).archive().as[Root]
      withClue(result.archivedAt.value → Instant.now) {
        result.archivedAt.value.isBeforeNow must === (true)
      }
    }

    "Archived product must be inactive" in new Fixture {
      val attributes = productsApi(product.formId).archive().as[Root].attributes

      val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
      val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]

      activeTo must === (None)
      activeFrom must === (None)
    }

    "Returns error if product is present in carts" in new Fixture {
      val cart = cartsApi.create(CreateCart(email = "yax@yax.com".some)).as[CartResponse]

      cartsApi(cart.referenceNumber).lineItems
        .add(Seq(UpdateLineItemsPayload(skuRedSmallCode, 1)))
        .mustBeOk()

      productsApi(product.formId)
        .archive()
        .mustFailWith400(ProductIsPresentInCarts(product.formId))
    }

    "Variants must be unlinked" in new VariantFixture {
      productsApi(product.formId).archive().as[Root].variants mustBe empty
    }

    "Product options must be unlinked" in new VariantFixture {
      productsApi(product.formId).archive().as[Root].options mustBe empty
    }

    "Albums must be unlinked" in new VariantFixture {
      productsApi(product.formId).archive().as[Root].albums mustBe empty
    }

    "Responds with NOT FOUND when wrong product is requested" in new VariantFixture {
      productsApi(666).archive().mustFailWith404(ProductFormNotFoundForContext(666, ctx.id))
    }

    "Responds with NOT FOUND when wrong context is requested" in new VariantFixture {
      implicit val donkeyContext = ObjectContext(name = "donkeyContext", attributes = JNothing)
      productsApi(product.formId)(donkeyContext)
        .archive()
        .mustFailWith404(ObjectContextNotFound("donkeyContext"))
    }
  }

  trait Fixture extends StoreAdmin_Seed with Schemas_Seed {

    def makeVariantPayload(code: String,
                           name: String,
                           albums: Option[Seq[AlbumPayload]]): ProductVariantPayload = {
      val attrMap = Map("title" → (("t" → "string") ~ ("v" → name)),
                        "name" → (("t" → "string") ~ ("v" → name)),
                        "code" → (("t" → "string") ~ ("v" → code)))

      ProductVariantPayload(attributes = attrMap, albums = albums)
    }

    def makeVariantPayload(code: String,
                           attrMap: Map[String, Json],
                           albums: Option[Seq[AlbumPayload]]) = {
      val codeJson   = ("t"               → "string") ~ ("v"      → code)
      val titleJson  = ("t"               → "string") ~ ("v"      → ("title_" + code))
      val attributes = (attrMap + ("code" → codeJson)) + ("title" → titleJson)
      ProductVariantPayload(attributes = attributes, albums = albums)
    }

    val priceValue = ("currency" → "USD") ~ ("value" → 9999)
    val priceJson  = ("t" → "price") ~ ("v" → priceValue)
    val skuAttrMap = Map("price" → priceJson)
    val skuPayload = makeVariantPayload("SKU-NEW-TEST", skuAttrMap, None)

    val nameJson = ("t"       → "string") ~ ("v"  → "Product name")
    val attrMap  = Map("name" → nameJson, "title" → nameJson)
    val productPayload = CreateProductPayload(attributes = attrMap,
                                              variants = Seq(skuPayload),
                                              options = None,
                                              albums = None)

    val simpleProd = SimpleProductData(title = "Test Product",
                                       code = "TEST",
                                       description = "Test product description",
                                       image = "image.png",
                                       price = 5999)

    val skuRedSmallCode: String   = "SKU-RED-SMALL"
    val skuRedLargeCode: String   = "SKU-RED-LARGE"
    val skuGreenSmallCode: String = "SKU-GREEN-SMALL"
    val skuGreenLargeCode: String = "SKU-GREEN-LARGE"

    val allSkus: Seq[String] =
      Seq(skuRedSmallCode, skuRedLargeCode, skuGreenSmallCode, skuGreenLargeCode)

    val simpleVariants = Seq(
        SimpleVariant(skuRedSmallCode, "A small, red item", 9999, Currency.USD),
        SimpleVariant(skuRedLargeCode, "A large, red item", 9999, Currency.USD),
        SimpleVariant(skuGreenSmallCode, "A small, green item", 9999, Currency.USD),
        SimpleVariant(skuGreenLargeCode, "A large, green item", 9999, Currency.USD))

    val variantsWithValues = Seq(
        SimpleCompleteOption(
            SimpleProductOption("Size"),
            Seq(SimpleProductValue("small", ""), SimpleProductValue("large", ""))),
        SimpleCompleteOption(
            SimpleProductOption("Color"),
            Seq(SimpleProductValue("red", "ff0000"), SimpleProductValue("green", "00ff00"))))

    val skuValueMapping: Seq[(String, String, String)] = Seq((skuRedSmallCode, "red", "small"),
                                                             (skuRedLargeCode, "red", "large"),
                                                             (skuGreenSmallCode, "green", "small"),
                                                             (skuGreenLargeCode, "green", "large"))

    val (product, skus, variants) = ({
      val scope = Scope.current

      for {
        // Create the SKUs.
        skus ← * <~ Mvp.insertVariants(scope, ctx.id, simpleVariants)

        _ ← * <~ ProductVariantMwhSkuIds.createAll(skus.map { variant ⇒
             ProductVariantMwhSkuId(variantFormId = variant.formId, mwhSkuId = variant.formId)
           })

        // Create the product.
        product ← * <~ Mvp.insertProductWithExistingSkus(scope, ctx.id, simpleProd, skus)

        // Create the ProductOptions and their Values.
        variantsAndValues ← * <~ variantsWithValues.map { scv ⇒
                             Mvp.insertVariantWithValues(scope, ctx.id, product, scv)
                           }

        variants ← * <~ variantsAndValues.map(_.variant)
        variantValues ← * <~ variantsAndValues.flatMap(_.variantValues)

        // Map the SKUs to the ProductOption Values
        skuMap ← * <~ skuValueMapping.map {
                  case (code, colorName, sizeName) ⇒
                    val selectedSku = skus.filter(_.code == code).head
                    val colorValue  = variantValues.filter(_.name == colorName).head
                    val sizeValue   = variantValues.filter(_.name == sizeName).head

                    for {
                      colorLink ← * <~ ProductValueVariantLinks.create(
                                     ProductValueVariantLink(leftId = colorValue.valueId,
                                                             rightId = selectedSku.id))
                      sizeLink ← * <~ ProductValueVariantLinks.create(
                                    ProductValueVariantLink(leftId = sizeValue.valueId,
                                                            rightId = selectedSku.id))
                    } yield (colorLink, sizeLink)
                }
      } yield (product, skus, variantsAndValues)
    }).gimme
  }

  trait VariantFixture extends Fixture {
    def productOptionPayload(name: String, values: Seq[ProductOptionValuePayload]) =
      ProductOptionPayload(attributes = Map("name" → (("t" → "string") ~ ("v" → name))),
                           values = Some(values))

    val redSkus   = Seq(skuRedSmallCode, skuRedLargeCode)
    val greenSkus = Seq(skuGreenSmallCode, skuGreenLargeCode)
    val smallSkus = Seq(skuRedSmallCode, skuGreenSmallCode)
    val largeSkus = Seq(skuRedLargeCode, skuGreenLargeCode)

    val redValuePayload =
      ProductOptionValuePayload(name = Some("Red"), swatch = Some("ff0000"), skuCodes = Seq.empty)
    val greenValuePayload = ProductOptionValuePayload(name = Some("Green"),
                                                      swatch = Some("00ff00"),
                                                      skuCodes = Seq.empty)

    val justColorVariantPayload = productOptionPayload(
        "Color",
        Seq(redValuePayload.copy(skuCodes = Seq(skuRedSmallCode)),
            greenValuePayload.copy(skuCodes = Seq(skuGreenSmallCode))))

    val smallValuePayload =
      ProductOptionValuePayload(name = Some("Small"), swatch = None, skuCodes = Seq.empty)

    val largeValuePayload =
      ProductOptionValuePayload(name = Some("Large"), swatch = None, skuCodes = Seq.empty)

    val justSizeVariantPayload = productOptionPayload(
        "Size",
        Seq(smallValuePayload.copy(skuCodes = Seq(skuRedSmallCode)),
            largeValuePayload.copy(skuCodes = Seq(skuRedLargeCode))))

    private val colorVariantPayload = productOptionPayload(
        "Color",
        Seq(redValuePayload.copy(skuCodes = redSkus),
            greenValuePayload.copy(skuCodes = greenSkus)))

    private val sizeVariantPayload = productOptionPayload(
        "Size",
        Seq(smallValuePayload.copy(skuCodes = smallSkus),
            largeValuePayload.copy(skuCodes = largeSkus)))

    val colorSizeVariants = Seq(colorVariantPayload, sizeVariantPayload)

    val smallRedSkuPayload   = makeVariantPayload(skuRedSmallCode, "A small, red item", None)
    val smallGreenSkuPayload = makeVariantPayload(skuGreenSmallCode, "A small, green item", None)
    val largeRedSkuPayload   = makeVariantPayload(skuRedLargeCode, "A small, green item", None)
    val largeGreenSkuPayload = makeVariantPayload(skuGreenLargeCode, "A large, green item", None)
  }

  trait ArchivedSkuFixture extends VariantFixture {

    val archivedSkus = (for {
      archivedSkus ← * <~ skus.map { sku ⇒
                      ProductVariants.update(sku, sku.copy(archivedAt = Some(Instant.now)))
                    }
    } yield archivedSkus).gimme

    val archivedSkuCode           = "SKU-RED-SMALL"
    val archivedSkuProductPayload = productPayload.copy(variants = Seq(smallRedSkuPayload))
  }

  trait RemovingSkusFixture extends VariantFixture {

    val twoSkuVariantPayload: Seq[ProductOptionPayload] = Seq(
        productOptionPayload("Size",
                             Seq(redValuePayload.copy(skuCodes = Seq(skuRedLargeCode)),
                                 greenValuePayload.copy(skuCodes = Seq(skuGreenSmallCode)))),
        productOptionPayload("Color",
                             Seq(smallValuePayload.copy(skuCodes = Seq(skuGreenSmallCode)),
                                 largeValuePayload.copy(skuCodes = Seq(skuRedLargeCode)))))

    val twoSkuPayload: Seq[ProductVariantPayload] = Seq(
        makeVariantPayload(skuRedLargeCode, "A large, red item", None),
        makeVariantPayload(skuGreenSmallCode, "A small, green item", None))

    val twoSkuProductPayload: UpdateProductPayload = UpdateProductPayload(
        attributes = attrMap,
        options = twoSkuVariantPayload.some,
        albums = None,
        variants = twoSkuPayload.some)
  }
}
