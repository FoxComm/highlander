import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import cats.implicits._
import failures.ArchiveFailures._
import failures.ObjectFailures.ObjectContextNotFound
import failures.ProductFailures
import failures.ProductFailures._
import models.account.Scope
import models.inventory.Skus
import models.objects._
import models.product._
import org.json4s.JsonDSL._
import org.json4s._
import payloads.ImagePayloads._
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.CartPayloads.CreateCart
import payloads.ProductPayloads._
import payloads.SkuPayloads.SkuPayload
import payloads.VariantPayloads.{VariantPayload, VariantValuePayload}
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
    "returns assigned taxonomies" in new ProductAndSkus_Baked with FlatTaxons_Baked {
      taxonsApi(taxons.head.formId).assignProduct(simpleProduct.formId).mustBeOk()
      val product = productsApi(simpleProduct.formId).get().as[ProductResponse.Root]
      product.taxons.flatMap(_.taxons.map(_.id)) must contain(taxons.head.formId)
    }

    "queries product by slug" in new ProductSku_ApiFixture {
      val slug          = "simple-product"
      val simpleProduct = Products.mustFindById404(product.id).gimme

      val updated = simpleProduct.copy(slug = slug)

      productsApi(product.id)
        .update(
            UpdateProductPayload(productPayload.attributes,
                                 slug = Some(slug),
                                 skus = None,
                                 variants = None))
        .mustBeOk()

      productsApi(slug).get().as[ProductResponse.Root].id must === (updated.formId)
    }

    "queries product by slug ignoring case" in new ProductSku_ApiFixture {
      val slug          = "Simple-Product"
      val simpleProduct = Products.mustFindById404(product.id).gimme
      val updated       = simpleProduct.copy(slug = slug.toLowerCase)

      Products.update(simpleProduct, updated).gimme

      productsApi(slug).get().as[ProductResponse.Root].id must === (updated.formId)
    }
  }

  "GET v1/my/products/:ref/baked" - {
    "404 for archived products" in new ProductSku_ApiFixture {
      val slug = "simple-product"

      productsApi(product.id)
        .update(
            UpdateProductPayload(productPayload.attributes,
                                 slug = slug.some,
                                 skus = None,
                                 variants = None))
        .mustBeOk()

      productsApi(product.id).archive().mustBeOk()

      productsApi(slug).forCustomer.get.mustFailWith404(ProductIsNotActive(ProductReference(slug)))
    }

    "404 for inactive products" in new Customer_Seed with Fixture {
      val slug = "simple-product"

      productsApi(product.formId)
        .update(
            UpdateProductPayload(attributes = inactiveAttrMap,
                                 slug = slug.some,
                                 skus =
                                   allSkus.map(sku ⇒ makeSkuPayload(sku, skuAttrMap, None)).some,
                                 albums = None,
                                 variants = None))
        .mustBeOk()

      productsApi(slug).forCustomer.get.mustFailWith404(ProductIsNotActive(ProductReference(slug)))
    }
  }

  "POST v1/products/:context" - {
    def doQuery(productPayload: CreateProductPayload) = {
      productsApi.create(productPayload).as[Root]
    }

    "Creates a product with" - {
      val skuName = "SKU-NEW-TEST"

      "slug successfully" in new Fixture {
        val possibleSlug = List("simple-product", "1-Product", "p", "111something")
        for (slug ← possibleSlug) {
          val slugClue = s"slug: $slug"

          val productResponse = doQuery(productPayload.copy(slug = slug))
          productResponse.slug must === (slug.toLowerCase).withClue(slugClue)

          val getProductResponse = productsApi(slug).get().as[Root]
          getProductResponse.slug must === (slug.toLowerCase).withClue(slugClue)
          getProductResponse.id must === (productResponse.id).withClue(slugClue)
        }
      }

      "generates slug if it is empty" in new Fixture {
        val slug = ""

        val productResponse = doQuery(productPayload.copy(slug = slug))
        productResponse.slug.isEmpty must === (false)

        val generatedSlug      = productResponse.slug
        val getProductResponse = productsApi(generatedSlug).get().as[Root]
        getProductResponse.slug must === (generatedSlug)
        getProductResponse.id must === (productResponse.id)
      }

      "generated slug is unique" in new Fixture {
        val productResponses = for (_ ← 1 to 2)
          yield doQuery(productPayload.copy(slug = ""))

        val slugs = productResponses.map(_.slug)

        slugs.forall(!_.isEmpty) must === (true)
        slugs.distinct.size must === (slugs.size)
      }

      "a new SKU successfully" in new Fixture {
        val productResponse = doQuery(productPayload)
        productResponse.skus.length must === (1)
        productResponse.skus.head.attributes.code must === (skuName)
      }

      "a new SKU in variant successfully" in new Fixture {
        val valuePayload =
          Seq(VariantValuePayload(skuCodes = Seq(skuName), swatch = None, name = Some("Test")))
        val variantPayload =
          Seq(VariantPayload(attributes = Map("test" → "Test"), values = Some(valuePayload)))

        val productResponse = doQuery(productPayload.copy(variants = Some(variantPayload)))

        productResponse.skus.length must === (1)
        productResponse.skus.head.attributes.code must === (skuName)

        productResponse.variants.size must === (1)
        val variant = productResponse.variants.head
        variant.values.size must === (1)
        variant.values.head.skuCodes must contain only skuName
      }

      "Gets an associated SKU after creating a product with a SKU" in new Fixture {
        val redSkuPayload = makeSkuPayload("SKU-RED-SMALL", skuAttrMap, None)
        val payload       = productPayload.copy(skus = Seq(redSkuPayload))

        val productResponse    = productsApi.create(payload).as[Root]
        val getProductResponse = productsApi(productResponse.id).get().as[Root]
        getProductResponse.skus.length must === (1)

        val getFirstSku :: Nil = getProductResponse.skus
        val getCode            = getFirstSku.attributes \ "code" \ "v"
        getCode.extract[String] must === ("SKU-RED-SMALL")
      }

      "an existing SKU successfully" in new Fixture {
        val redSkuPayload = makeSkuPayload(skuRedSmallCode, skuAttrMap, None)
        val payload       = productPayload.copy(skus = Seq(redSkuPayload))

        val productResponse = doQuery(payload)

        productResponse.skus.length must === (1)
        productResponse.skus.head.attributes.code must === (skuRedSmallCode)
      }

      "an existing SKU with variants successfully" in new VariantFixture {
        val payload         = productPayload.copy(variants = Some(Seq(justColorVariantPayload)))
        val productResponse = doQuery(payload)

        productResponse.variants.length must === (1)
        productResponse.variants.head.values.length must === (2)

        val skuCodes        = productResponse.skus.map(s ⇒ s.attributes.code)
        val variantSkuCodes = productResponse.variants.head.values.flatMap(v ⇒ v.skuCodes)
        val expectedSkus    = justColorVariantPayload.values.getOrElse(Seq.empty).flatMap(_.skuCodes)

        skuCodes must contain only (expectedSkus: _*)
        variantSkuCodes must contain only (expectedSkus: _*)
      }

      "an existing, but modified SKU successfully" in new Fixture {
        val redPriceJson  = ("t" → "price") ~ ("v" → (("currency" → "USD") ~ ("value" → 7999)))
        val redSkuAttrMap = Map("salePrice" → redPriceJson)
        val src           = "http://lorempixel/test.png"
        val imagePayload  = ImagePayload(src = src)
        val albumPayload  = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

        val redSkuPayload = makeSkuPayload(skuRedLargeCode, redSkuAttrMap, Seq(albumPayload).some)
        val payload       = productPayload.copy(skus = Seq(redSkuPayload))

        val productResponse = doQuery(payload)

        productResponse.skus.length must === (1)
        productResponse.skus.head.attributes.code must === (skuRedLargeCode)
      }

      "empty variant successfully" in new Fixture {
        val redSkuPayload = makeSkuPayload(skuRedSmallCode, skuAttrMap, None)
        val values =
          Seq(VariantValuePayload(name = Some("value"), swatch = None, skuCodes = Seq.empty))
        val variantPayload =
          Seq(VariantPayload(attributes = Map("t" → "t"), values = Some(values)))
        val payload =
          productPayload.copy(skus = Seq(redSkuPayload), variants = Some(variantPayload))

        val response = doQuery(payload)

        response.variants.length must === (1)
        response.variants.head.values.length must === (1)
        response.variants.head.values.head.skuCodes.length must === (0)
        response.skus.length must === (0)
      }

      "an album successfully" in new Fixture {
        val src          = "http://lorempixel/test.png"
        val imagePayload = ImagePayload(src = src)
        val albumPayload = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

        val productResponse = doQuery(productPayload.copy(albums = Seq(albumPayload).some))
        productResponse.albums.length must === (1)
        productResponse.albums.head.images.length must === (1)
        productResponse.albums.head.images.head.src must === (src)

        val getProductResponse = productsApi(productResponse.id).get().as[Root]
        getProductResponse.albums.length must === (1)
        getProductResponse.albums.head.images.length must === (1)
        getProductResponse.albums.head.images.head.src must === (src)
      }

      "a SKU with an album successfully" in new Fixture {
        val src          = "http://lorempixel/test.png"
        val imagePayload = ImagePayload(src = src)
        val albumPayload = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

        val newSkuPayload     = productPayload.skus.head.copy(albums = Seq(albumPayload).some)
        val newProductPayload = productPayload.copy(skus = Seq(newSkuPayload))

        val productResponse = doQuery(newProductPayload)
        productResponse.skus.length must === (1)
        productResponse.skus.head.albums.length must === (1)

        val getProductResponse = productsApi(productResponse.id).get().as[Root]
        getProductResponse.skus.length must === (1)

        val album :: Nil = getProductResponse.skus.head.albums
        album.images.length must === (1)
        album.images.head.src must === (src)
      }
    }

    "Throws an error if" - {
      "no SKU is added" in new Fixture {
        val payload = productPayload.copy(skus = Seq.empty)
        productsApi.create(payload).mustFailWithMessage("SKUs must not be empty")
      }

      "no SKU exists for variants" in new Fixture {
        val values = Seq(
            VariantValuePayload(name = Some("name"),
                                swatch = None,
                                skuCodes = Seq("SKU-TEST1", "SKU-TEST2")))
        val variantPayload =
          Seq(VariantPayload(attributes = Map("t" → "t"), values = Some(values)))
        val payload = productPayload.copy(skus = Seq.empty, variants = Some(variantPayload))

        productsApi.create(payload).mustFailWithMessage("SKUs must not be empty")
      }

      "there is more than one SKU and no variants" in new Fixture {
        val sku1    = makeSkuPayload("SKU-TEST-NUM1", attrMap, None)
        val sku2    = makeSkuPayload("SKU-TEST-NUM2", attrMap, None)
        val payload = productPayload.copy(skus = Seq(sku1, sku2), variants = Some(Seq.empty))

        productsApi.create(payload).mustFailWithMessage("number of SKUs got 2, expected 1 or less")
      }

      "trying to create a product and SKU with no code" in new Fixture {
        val newProductPayload =
          productPayload.copy(skus = Seq(SkuPayload(attributes = skuAttrMap, albums = None)))

        productsApi.create(newProductPayload).mustFailWithMessage("SKU code not found in payload")
      }

      "trying to create a product and SKU with empty code" in new Fixture {
        val newProductPayload =
          productPayload.copy(skus = Seq(makeSkuPayload("", skuAttrMap, None)))

        productsApi
          .create(newProductPayload)
          .mustFailWithMessage(
              """Object sku with id=13 doesn't pass validation: $.code: must be at least 1 characters long""")
      }

      "trying to create a product with archived SKU" in new ArchivedSkuFixture {
        productsApi
          .create(archivedSkuProductPayload)
          .mustFailWith400(LinkArchivedSkuFailure(Product, 2, archivedSkuCode))
      }

      "trying to create a product with string price" in new Fixture {
        val price: Json = ("t" → "price") ~ ("v" → (("currency"
                        → "USD") ~ ("value" → "1000")))
        val skuAttributes: Map[String, Json] = skuPayload.attributes + ("salePrice" → price)
        val productToCreate =
          productPayload.copy(skus = Seq(skuPayload.copy(attributes = skuAttributes)))
        val createResponse = productsApi.create(productToCreate)

        createResponse.mustHaveStatus(StatusCodes.BadRequest)
        val errorPattern =
          "Object sku with id=\\d+ doesn't pass validation: \\$.salePrice.value: string found, number expected"
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
      val productId = doQuery(productPayload).id

      val response = productsApi(productId).get().as[Root]
      response.skus.length must === (1)
      response.skus.head.attributes.code must === ("SKU-NEW-TEST")
    }
  }

  "PATCH v1/products/:context/:id" - {
    def doQuery(formId: Int, productPayload: UpdateProductPayload)(implicit sl: SL, sf: SF) = {
      productsApi(formId).update(productPayload).as[Root]
    }

    "Doesn't complain if you do update w/o any changes" in new Customer_Seed with Fixture {
      private val cartRef =
        cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

      cartsApi(cartRef).lineItems.add(allSkus.map(sku ⇒ UpdateLineItemsPayload(sku, 1))).mustBeOk()

      productsApi(product.formId)
        .update(
            UpdateProductPayload(attributes = attrMap,
                                 skus =
                                   allSkus.map(sku ⇒ makeSkuPayload(sku, skuAttrMap, None)).some,
                                 albums = None,
                                 variants = None))
        .mustBeOk()
    }

    "Updates slug successfully" in new Fixture {
      val slug = "simple-product"

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         slug = slug.some,
                                         skus = Some(Seq(skuPayload)),
                                         variants = None,
                                         albums = None)

      doQuery(product.formId, payload).slug must === (slug)
    }

    "Updates uppercase slug successfully" in new Fixture {
      val slug = "Simple-Product"

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         slug = slug.some,
                                         skus = Some(Seq(skuPayload)),
                                         variants = None,
                                         albums = None)

      doQuery(product.formId, payload).slug must === (slug.toLowerCase)
    }

    "Updates the SKUs on a product successfully" in new Fixture {
      val payload = UpdateProductPayload(attributes = Map.empty,
                                         skus = Some(Seq(skuPayload)),
                                         variants = None,
                                         albums = None)

      val response = doQuery(product.formId, payload)
      response.skus.length must === (4)
      response.variants.length must === (2)

      val description = response.attributes \ "description" \ "v"
      description.extract[String] must === ("Test product description")
    }

    "Updates a SKU with an album successfully" in new Fixture with Product_Raw {
      val src           = "http://lorempixel/test.png"
      val imagePayload  = ImagePayload(src = src)
      val albumPayload  = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)
      val albumsPayload = Seq(albumPayload).some

      val updateSkuPayload = makeSkuPayload("SKU-UPDATE-TEST", skuAttrMap, albumsPayload)
      val newAttrMap       = Map("name" → (("t" → "string") ~ ("v" → "Some new product name")))
      val payload = UpdateProductPayload(attributes = newAttrMap,
                                         skus = Some(Seq(updateSkuPayload)),
                                         albums = None,
                                         variants = Some(Seq.empty))

      val response = doQuery(simpleProduct.formId, payload)
      response.skus.length must === (1)

      val getProductResponse = productsApi(response.id).get().as[Root]
      getProductResponse.skus.length must === (1)

      val sku :: Nil = getProductResponse.skus
      sku.albums.length must === (1)

      val album :: Nil = sku.albums
      album.images.length must === (1)
      album.images.head.src must === (src)
    }

    "Updates an album on a product" in new Fixture {
      val src          = "http://lorempixel/test.png"
      val imagePayload = ImagePayload(src = src)
      val albumPayload = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

      val newSkuPayload = productPayload.skus.head.copy(albums = Seq(albumPayload).some)

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         skus = Seq(newSkuPayload).some,
                                         variants = Seq.empty.some,
                                         albums = Seq(albumPayload).some)

      val productResponse = productsApi(product.formId).update(payload).as[Root]
      productResponse.albums.length must === (1)

      val getProductResponse = productsApi(productResponse.id).get().as[Root]
      getProductResponse.albums.length must === (1)

      val album :: Nil = getProductResponse.albums
      album.images.length must === (1)
      album.images.head.src must === (src)

    }

    "Updates and replaces a SKU on the product" in new Fixture with Product_Raw {
      val updateSkuPayload = makeSkuPayload("SKU-UPDATE-TEST", skuAttrMap, None)
      val newAttrMap       = Map("name" → (("t" → "string") ~ ("v" → "Some new product name")))
      val payload = UpdateProductPayload(attributes = newAttrMap,
                                         skus = Some(Seq(updateSkuPayload)),
                                         albums = None,
                                         variants = Some(Seq.empty))

      val response = doQuery(simpleProduct.formId, payload)
      response.skus.length must === (1)
      response.variants.length must === (0)

      val skuResponse = response.skus.head
      val code        = skuResponse.attributes \ "code" \ "v"
      code.extract[String] must === ("SKU-UPDATE-TEST")

      val description = response.attributes \ "description" \ "v"
      description.extract[String] must === ("Test product description")

      val name = response.attributes \ "name" \ "v"
      name.extract[String] must === ("Some new product name")
    }

    "Removes SKUs from product" in new Fixture {
      productsApi(product.formId)
        .update(
            UpdateProductPayload(attributes = attrMap,
                                 skus = Seq.empty.some,
                                 albums = None,
                                 variants = Seq.empty.some))
        .as[Root]
        .skus mustBe empty
    }

    "Removes some SKUs from product" in new RemovingSkusFixture {
      productsApi(product.formId).get.as[Root].skus must have size 4

      val remainingSkus: Seq[String] = productsApi(product.formId)
        .update(twoSkuProductPayload)
        .as[Root]
        .skus
        .map(sku ⇒ (sku.attributes \ "code" \ "v").extract[String])

      remainingSkus must have size 2
      remainingSkus must contain theSameElementsAs Seq(skuRedLargeCode, skuGreenSmallCode)
    }

    "Updates the SKUs on a product if variants are Some(Seq.empty)" in new Fixture {

      ProductSkuLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme
      ProductVariantLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         skus = Some(Seq(skuPayload)),
                                         albums = None,
                                         variants = Some(Seq.empty))

      val response = doQuery(product.formId, payload)
      response.skus.length must === (1)
      response.variants.length must === (0)

      val description = response.attributes \ "description" \ "v"
      description.extract[String] must === ("Test product description")
    }

    "Multiple calls with same params create single SKU link" in new Fixture {

      ProductSkuLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme
      ProductVariantLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         skus = Some(Seq(skuPayload)),
                                         variants = None,
                                         albums = None)

      var response = doQuery(product.formId, payload)
      response.skus.length must === (1)

      response = doQuery(product.formId, payload)
      response.skus.length must === (1)

      ProductSkuLinks.filterLeft(product).gimme.size must === (1)
    }

    "Updates the properties on a product successfully" in new Fixture {
      val newAttrMap = Map("name" → (("t" → "string") ~ ("v" → "Some new product name")))
      val payload =
        UpdateProductPayload(attributes = newAttrMap, skus = None, variants = None, albums = None)

      val response = doQuery(product.formId, payload)
      response.skus.length must === (4)
      response.variants.length must === (2)
    }

    "Updates the variants" - {
      "Remove all variants successfully" in new Fixture {
        val payload = UpdateProductPayload(attributes = Map.empty,
                                           skus = None,
                                           variants = Some(Seq()),
                                           albums = None)

        val response = doQuery(product.formId, payload)
        response.skus.length must === (0)
        response.variants.length must === (0)
      }

      "Add new variant with new SKU successfully" in new VariantFixture {
        private val newSkuCode: ActivityType = "SKU-NEW-TEST"
        val newSkuPayload                    = makeSkuPayload(newSkuCode, skuAttrMap, None)

        val goldValuePayload =
          VariantValuePayload(name = Some("Gold"), swatch = None, skuCodes = Seq(newSkuCode))
        val silverValuePayload =
          VariantValuePayload(name = Some("Silver"), swatch = None, skuCodes = Seq.empty)
        val metalVariantPayload =
          makeVariantPayload("Metal", Seq(goldValuePayload, silverValuePayload))

        val payload = UpdateProductPayload(attributes = Map.empty,
                                           skus = Some(Seq(newSkuPayload)),
                                           albums = None,
                                           variants =
                                             Some(colorSizeVariants.+:(metalVariantPayload)))

        val response = doQuery(product.formId, payload)
        response.skus.length must === (5)
        response.variants.length must === (3)
        response.skus.map(_.attributes.code) must contain(newSkuCode)
      }
    }

    "Throws an error" - {
      "if updating adds too many SKUs" in new VariantFixture {
        val upPayload = UpdateProductPayload(
            attributes = Map.empty,
            skus = Some(
                Seq(skuPayload,
                    smallRedSkuPayload,
                    smallGreenSkuPayload,
                    largeRedSkuPayload,
                    largeGreenSkuPayload)),
            variants = None,
            albums = None
        )

        productsApi(product.formId)
          .update(upPayload)
          .mustFailWithMessage("number of SKUs for given variants got 5, expected 4 or less")
      }

      "trying to update a product with archived SKU" in new ArchivedSkuFixture {
        productsApi(product.formId)
          .update(UpdateProductPayload(attributes = archivedSkuProductPayload.attributes,
                                       skus = archivedSkuProductPayload.skus.some,
                                       albums = None,
                                       variants = archivedSkuProductPayload.variants))
          .mustFailWith400(LinkArchivedSkuFailure(Product, product.id, archivedSkuCode))
      }

      "trying to unassociate a SKU that is in cart" in new RemovingSkusFixture {
        val cartRefNum =
          cartsApi.create(CreateCart(email = "yax@yax.com".some)).as[CartResponse].referenceNumber

        cartsApi(cartRefNum).lineItems
          .add(Seq(UpdateLineItemsPayload(skuGreenLargeCode, 1)))
          .mustBeOk()

        productsApi(product.formId)
          .update(twoSkuProductPayload)
          .mustFailWith400(SkuIsPresentInCarts(skuGreenLargeCode))
      }

      "slug is invalid" in new Fixture {
        val createdProduct = productsApi.create(productPayload).as[Root]

        val invalidSlugValues = Seq("1", "-1", "+1", "_", "-")
        for (slug ← invalidSlugValues) {
          productsApi(createdProduct.id)
            .update(UpdateProductPayload(attributes = productPayload.attributes,
                                         slug = Some(slug),
                                         skus = None,
                                         variants = None))
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
                                 skus = None,
                                 variants = None))

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

    "SKUs must be unlinked" in new VariantFixture {
      productsApi(product.formId).archive().as[Root].skus mustBe empty
    }

    "Variants must be unlinked" in new VariantFixture {
      productsApi(product.formId).archive().as[Root].variants mustBe empty
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

    def makeSkuPayload(code: String, name: String, albums: Option[Seq[AlbumPayload]]): SkuPayload = {
      val attrMap = Map("title" → (("t" → "string") ~ ("v" → name)),
                        "name" → (("t" → "string") ~ ("v" → name)),
                        "code" → (("t" → "string") ~ ("v" → code)))

      SkuPayload(attributes = attrMap, albums = albums)
    }

    def makeSkuPayload(code: String,
                       attrMap: Map[String, Json],
                       albums: Option[Seq[AlbumPayload]]) = {
      val codeJson   = ("t"               → "string") ~ ("v"      → code)
      val titleJson  = ("t"               → "string") ~ ("v"      → ("title_" + code))
      val attributes = (attrMap + ("code" → codeJson)) + ("title" → titleJson)
      SkuPayload(attributes = attributes, albums = albums)
    }

    val priceValue = ("currency" → "USD") ~ ("value" → 9999)
    val priceJson  = ("t" → "price") ~ ("v" → priceValue)
    val skuAttrMap = Map("price" → priceJson)
    val skuPayload = makeSkuPayload("SKU-NEW-TEST", skuAttrMap, None)

    val nameJson        = ("t"                        → "string") ~ ("v"           → "Product name")
    val attrMap         = Map("name"                  → nameJson, "title"          → nameJson)
    val activeFromJson  = ("t"                        → "date") ~ ("v"             → (Instant.now.minus(2, ChronoUnit.DAYS)).toString)
    val activeToJson    = ("t"                        → "date") ~ ("v"             → (Instant.now.minus(1, ChronoUnit.DAYS)).toString)
    val inactiveAttrMap = attrMap ++ Map("activeFrom" → activeFromJson, "activeTo" → activeToJson)
    val productPayload = CreateProductPayload(attributes = attrMap,
                                              skus = Seq(skuPayload),
                                              variants = None,
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

    val simpleSkus = Seq(SimpleSku(skuRedSmallCode, "A small, red item", 9999, Currency.USD),
                         SimpleSku(skuRedLargeCode, "A large, red item", 9999, Currency.USD),
                         SimpleSku(skuGreenSmallCode, "A small, green item", 9999, Currency.USD),
                         SimpleSku(skuGreenLargeCode, "A large, green item", 9999, Currency.USD))

    val variantsWithValues = Seq(
        SimpleCompleteVariant(
            SimpleVariant("Size"),
            Seq(SimpleVariantValue("small", ""), SimpleVariantValue("large", ""))),
        SimpleCompleteVariant(
            SimpleVariant("Color"),
            Seq(SimpleVariantValue("red", "ff0000"), SimpleVariantValue("green", "00ff00"))))

    val skuValueMapping: Seq[(String, String, String)] = Seq((skuRedSmallCode, "red", "small"),
                                                             (skuRedLargeCode, "red", "large"),
                                                             (skuGreenSmallCode, "green", "small"),
                                                             (skuGreenLargeCode, "green", "large"))

    val (product, skus, variants) = ({
      val scope = Scope.current

      for {
        // Create the SKUs.
        skus ← * <~ Mvp.insertSkus(scope, ctx.id, simpleSkus)

        // Create the product.
        product ← * <~ Mvp.insertProductWithExistingSkus(scope, ctx.id, simpleProd, skus)

        // Create the Variants and their Values.
        variantsAndValues ← * <~ variantsWithValues.map { scv ⇒
                             Mvp.insertVariantWithValues(scope, ctx.id, product, scv)
                           }

        variants ← * <~ variantsAndValues.map(_.variant)
        variantValues ← * <~ variantsAndValues.foldLeft(Seq.empty[SimpleVariantValueData]) {
                         (acc, item) ⇒
                           acc ++ item.variantValues
                       }

        // Map the SKUs to the Variant Values
        skuMap ← * <~ skuValueMapping.map {
                  case (code, colorName, sizeName) ⇒
                    val selectedSku = skus.filter(_.code == code).head
                    val colorValue  = variantValues.filter(_.name == colorName).head
                    val sizeValue   = variantValues.filter(_.name == sizeName).head

                    for {
                      colorLink ← * <~ VariantValueSkuLinks.create(
                                     VariantValueSkuLink(leftId = colorValue.valueId,
                                                         rightId = selectedSku.id))
                      sizeLink ← * <~ VariantValueSkuLinks.create(
                                    VariantValueSkuLink(leftId = sizeValue.valueId,
                                                        rightId = selectedSku.id))
                    } yield (colorLink, sizeLink)
                }
      } yield (product, skus, variantsAndValues)
    }).gimme
  }

  trait VariantFixture extends Fixture {
    def makeVariantPayload(name: String, values: Seq[VariantValuePayload]) =
      VariantPayload(attributes = Map("name" → (("t" → "string") ~ ("v" → name))),
                     values = Some(values))

    val redSkus   = Seq(skuRedSmallCode, skuRedLargeCode)
    val greenSkus = Seq(skuGreenSmallCode, skuGreenLargeCode)
    val smallSkus = Seq(skuRedSmallCode, skuGreenSmallCode)
    val largeSkus = Seq(skuRedLargeCode, skuGreenLargeCode)

    val redValuePayload =
      VariantValuePayload(name = Some("Red"), swatch = Some("ff0000"), skuCodes = Seq.empty)
    val greenValuePayload =
      VariantValuePayload(name = Some("Green"), swatch = Some("00ff00"), skuCodes = Seq.empty)

    val justColorVariantPayload = makeVariantPayload(
        "Color",
        Seq(redValuePayload.copy(skuCodes = Seq(skuRedSmallCode)),
            greenValuePayload.copy(skuCodes = Seq(skuGreenSmallCode))))

    val smallValuePayload =
      VariantValuePayload(name = Some("Small"), swatch = None, skuCodes = Seq.empty)

    val largeValuePayload =
      VariantValuePayload(name = Some("Large"), swatch = None, skuCodes = Seq.empty)

    val justSizeVariantPayload = makeVariantPayload(
        "Size",
        Seq(smallValuePayload.copy(skuCodes = Seq(skuRedSmallCode)),
            largeValuePayload.copy(skuCodes = Seq(skuRedLargeCode))))

    private val colorVariantPayload = makeVariantPayload(
        "Color",
        Seq(redValuePayload.copy(skuCodes = redSkus),
            greenValuePayload.copy(skuCodes = greenSkus)))

    private val sizeVariantPayload = makeVariantPayload(
        "Size",
        Seq(smallValuePayload.copy(skuCodes = smallSkus),
            largeValuePayload.copy(skuCodes = largeSkus)))

    val colorSizeVariants = Seq(colorVariantPayload, sizeVariantPayload)

    val smallRedSkuPayload   = makeSkuPayload(skuRedSmallCode, "A small, red item", None)
    val smallGreenSkuPayload = makeSkuPayload(skuGreenSmallCode, "A small, green item", None)
    val largeRedSkuPayload   = makeSkuPayload(skuRedLargeCode, "A small, green item", None)
    val largeGreenSkuPayload = makeSkuPayload(skuGreenLargeCode, "A large, green item", None)
  }

  trait ArchivedSkuFixture extends VariantFixture {

    val archivedSkus = (for {
      archivedSkus ← * <~ skus.map { sku ⇒
                      Skus.update(sku, sku.copy(archivedAt = Some(Instant.now)))
                    }
    } yield archivedSkus).gimme

    val archivedSkuCode           = "SKU-RED-SMALL"
    val archivedSkuProductPayload = productPayload.copy(skus = Seq(smallRedSkuPayload))
  }

  trait RemovingSkusFixture extends VariantFixture {

    val twoSkuVariantPayload: Seq[VariantPayload] = Seq(
        makeVariantPayload("Size",
                           Seq(redValuePayload.copy(skuCodes = Seq(skuRedLargeCode)),
                               greenValuePayload.copy(skuCodes = Seq(skuGreenSmallCode)))),
        makeVariantPayload("Color",
                           Seq(smallValuePayload.copy(skuCodes = Seq(skuGreenSmallCode)),
                               largeValuePayload.copy(skuCodes = Seq(skuRedLargeCode)))))

    val twoSkuPayload: Seq[SkuPayload] = Seq(
        makeSkuPayload(skuRedLargeCode, "A large, red item", None),
        makeSkuPayload(skuGreenSmallCode, "A small, green item", None))

    val twoSkuProductPayload: UpdateProductPayload = UpdateProductPayload(
        attributes = attrMap,
        variants = twoSkuVariantPayload.some,
        albums = None,
        skus = twoSkuPayload.some)
  }
}
