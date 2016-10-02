import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import util.Extensions._
import failures.ArchiveFailures._
import failures.NotFoundFailure404
import failures.ProductFailures._
import models.inventory.Skus
import models.objects._
import models.product._
import org.json4s.JsonDSL._
import org.json4s._
import payloads.ProductPayloads._
import payloads.SkuPayloads.SkuPayload
import payloads.VariantPayloads.{VariantPayload, VariantValuePayload}
import responses.ProductResponses.ProductResponse.Root
import responses.ProductResponses._
import util.{IntegrationTestBase, PhoenixAdminApi}
import util.fixtures.BakedFixtures
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

class ProductIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {
  import ProductTestExtensions._

  "POST v1/products/:context" - {
    def doQuery(productPayload: CreateProductPayload) = {
      productsApi.create(productPayload).as[Root]
    }

    "Creates a product with" - {
      val skuName = "SKU-NEW-TEST"

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
        val redSkuPayload = makeSkuPayload("SKU-RED-SMALL", skuAttrMap)
        val payload       = productPayload.copy(skus = Seq(redSkuPayload))

        val productResponse    = productsApi.create(payload).as[Root]
        val getProductResponse = productsApi(productResponse.id).get().as[Root]
        getProductResponse.skus.length must === (1)

        val getFirstSku :: Nil = getProductResponse.skus
        val getCode            = getFirstSku.attributes \ "code" \ "v"
        getCode.extract[String] must === ("SKU-RED-SMALL")
      }

      "an existing SKU successfully" in new Fixture {
        val redSkuPayload = makeSkuPayload(skuRedSmallCode, skuAttrMap)
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
        val redSkuPayload = makeSkuPayload(skuRedLargeCode, redSkuAttrMap)
        val payload       = productPayload.copy(skus = Seq(redSkuPayload))

        val productResponse = doQuery(payload)

        productResponse.skus.length must === (1)
        productResponse.skus.head.attributes.code must === (skuRedLargeCode)
      }

      "empty variant successfully" in new Fixture {
        val redSkuPayload = makeSkuPayload(skuRedSmallCode, skuAttrMap)
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
    }

    "Throws an error if" - {
      "no SKU is added" in new Fixture {
        val payload = productPayload.copy(skus = Seq.empty)

        val response = productsApi.create(payload)
        response.status must === (StatusCodes.BadRequest)
      }

      "no SKU exists for variants" in new Fixture {
        val values = Seq(
            VariantValuePayload(name = Some("name"),
                                swatch = None,
                                skuCodes = Seq("SKU-TEST1", "SKU-TEST2")))
        val variantPayload =
          Seq(VariantPayload(attributes = Map("t" → "t"), values = Some(values)))
        val payload = productPayload.copy(skus = Seq.empty, variants = Some(variantPayload))

        val response = productsApi.create(payload)
        response.status must === (StatusCodes.BadRequest)
      }

      "there is more than one SKU and no variants" in new Fixture {
        val sku1    = makeSkuPayload("SKU-TEST-NUM1", attrMap)
        val sku2    = makeSkuPayload("SKU-TEST-NUM2", attrMap)
        val payload = productPayload.copy(skus = Seq(sku1, sku2), variants = Some(Seq.empty))

        val response = productsApi.create(payload)
        response.status must === (StatusCodes.BadRequest)
      }

      "trying to create a product and SKU with no code" in new Fixture {
        val newSkuPayload     = SkuPayload(skuAttrMap)
        val newProductPayload = productPayload.copy(skus = Seq(newSkuPayload))

        val response = productsApi.create(newProductPayload)
        response.status must === (StatusCodes.BadRequest)
      }

      "trying to create a product and SKU with empty code" in new Fixture {
        val newSkuPayload     = makeSkuPayload("", skuAttrMap)
        val newProductPayload = productPayload.copy(skus = Seq(newSkuPayload))

        val response = productsApi.create(newProductPayload)
        response.status must === (StatusCodes.BadRequest)
      }

      "trying to create a product with archived SKU" in new ArchivedSkuFixture {
        val response = productsApi.create(archivedSkuProductPayload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (LinkArchivedSkuFailure(Product, 2, archivedSkuCode).description)
      }
    }

    "Creates a product then requests is successfully" in new Fixture {
      val productResponse = doQuery(productPayload)
      val productId       = productResponse.id

      val getProductResponse = productsApi(productId).get().as[Root]
      getProductResponse.skus.length must === (1)
      getProductResponse.skus.head.attributes.code must === ("SKU-NEW-TEST")
    }
  }

  "PATCH v1/products/:context/:id" - {
    def doQuery(formId: Int, productPayload: UpdateProductPayload) = {
      productsApi(formId).update(productPayload).as[Root]
    }

    "Updates the SKUs on a product successfully" in new Fixture {
      val payload =
        UpdateProductPayload(attributes = Map.empty, skus = Some(Seq(skuPayload)), variants = None)

      val response = doQuery(product.formId, payload)
      response.skus.length must === (4)
      response.variants.length must === (2)

      val description = response.attributes \ "description" \ "v"
      description.extract[String] must === ("Test product description")
    }

    "Updates and replaces a SKU on the product" in new Fixture with Product_Raw {
      val updateSkuPayload = makeSkuPayload("SKU-UPDATE-TEST", skuAttrMap)
      val newAttrMap       = Map("name" → (("t" → "string") ~ ("v" → "Some new product name")))
      val payload = UpdateProductPayload(attributes = newAttrMap,
                                         skus = Some(Seq(updateSkuPayload)),
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

    "Updates the SKUs on a product if variants are Some(Seq.empty)" in new Fixture {

      ProductSkuLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme
      ProductVariantLinks.filterLeft(product).deleteAll(DbResultT.none, DbResultT.none).gimme

      val payload = UpdateProductPayload(attributes = Map.empty,
                                         skus = Some(Seq(skuPayload)),
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

      val payload =
        UpdateProductPayload(attributes = Map.empty, skus = Some(Seq(skuPayload)), variants = None)

      var response = doQuery(product.formId, payload)
      response.skus.length must === (1)

      response = doQuery(product.formId, payload)
      response.skus.length must === (1)

      ProductSkuLinks.filterLeft(product).gimme.size must === (1)
    }

    "Updates the properties on a product successfully" in new Fixture {
      val newAttrMap = Map("name" → (("t" → "string") ~ ("v" → "Some new product name")))
      val payload    = UpdateProductPayload(attributes = newAttrMap, skus = None, variants = None)

      val response = doQuery(product.formId, payload)
      response.skus.length must === (4)
      response.variants.length must === (2)
    }

    "Updates the variants" - {
      "Remove all variants successfully" in new Fixture {
        val payload =
          UpdateProductPayload(attributes = Map.empty, skus = None, variants = Some(Seq()))

        val response = doQuery(product.formId, payload)
        response.skus.length must === (0)
        response.variants.length must === (0)
      }

      "Add new variant with new SKU successfully" in new VariantFixture {
        private val newSkuCode: ActivityType = "SKU-NEW-TEST"
        val newSkuPayload                    = makeSkuPayload(newSkuCode, skuAttrMap)

        val goldValuePayload =
          VariantValuePayload(name = Some("Gold"), swatch = None, skuCodes = Seq(newSkuCode))
        val silverValuePayload =
          VariantValuePayload(name = Some("Silver"), swatch = None, skuCodes = Seq.empty)
        val metalVariantPayload =
          makeVariantPayload("Metal", Seq(goldValuePayload, silverValuePayload))

        val payload = UpdateProductPayload(attributes = Map.empty,
                                           skus = Some(Seq(newSkuPayload)),
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
            variants = None
        )

        val response = productsApi(product.formId).update(upPayload)
        response.status must === (StatusCodes.BadRequest)
      }

      "trying to update a product with archived SKU" in new ArchivedSkuFixture {
        val response = productsApi(product.formId).update(
            UpdateProductPayload(attributes = archivedSkuProductPayload.attributes,
                                 skus = archivedSkuProductPayload.skus.some,
                                 variants = archivedSkuProductPayload.variants))

        response.status must === (StatusCodes.BadRequest)
        response.error must === (
            LinkArchivedSkuFailure(Product, product.id, archivedSkuCode).description)
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
      val response = productsApi(666).archive()

      response.status must === (StatusCodes.NotFound)
      response.error must === (ProductFormNotFoundForContext(666, ctx.id).description)
    }

    "Responds with NOT FOUND when wrong context is requested" in new VariantFixture {
      pending
      implicit val donkeyContext = ObjectContext(name = "donkeyContext", attributes = JNothing)
      val response               = productsApi(product.formId)(donkeyContext).archive()

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(ObjectContext, "donkeyContext").description)
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    def makeSkuPayload(code: String, name: String) = {
      val attrMap =
        Map("name" → (("t" → "string") ~ ("v" → name)), "code" → (("t" → "string") ~ ("v" → code)))
      SkuPayload(attrMap)
    }

    def makeSkuPayload(code: String, attrMap: Map[String, Json]) = {
      val codeJson = ("t" → "string") ~ ("v" → code)
      SkuPayload(attrMap + ("code" → codeJson))
    }

    val priceValue = ("currency" → "USD") ~ ("value" → 9999)
    val priceJson  = ("t" → "price") ~ ("v" → priceValue)
    val skuAttrMap = Map("price" → priceJson)
    val skuPayload = makeSkuPayload("SKU-NEW-TEST", skuAttrMap)

    val nameJson = ("t"       → "string") ~ ("v" → "Product name")
    val attrMap  = Map("name" → nameJson)
    val productPayload =
      CreateProductPayload(attributes = attrMap, skus = Seq(skuPayload), variants = None)

    val simpleProd = SimpleProductData(title = "Test Product",
                                       code = "TEST",
                                       description = "Test product description",
                                       image = "image.png",
                                       price = 5999)

    val skuRedSmallCode: String   = "SKU-RED-SMALL"
    val skuRedLargeCode: String   = "SKU-RED-LARGE"
    val skuGreenSmallCode: String = "SKU-GREEN-SMALL"
    val skuGreenLargeCode: String = "SKU-GREEN-LARGE"

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

    val (product, skus, variants) = (for {
      // Create the SKUs.
      skus ← * <~ Mvp.insertSkus(ctx.id, simpleSkus)

      // Create the product.
      product ← * <~ Mvp.insertProductWithExistingSkus(ctx.id, simpleProd, skus)

      // Create the Variants and their Values.
      variantsAndValues ← * <~ variantsWithValues.map { scv ⇒
                           Mvp.insertVariantWithValues(ctx.id, product, scv)
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
    } yield (product, skus, variantsAndValues)).gimme
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

    val smallRedSkuPayload   = makeSkuPayload(skuRedSmallCode, "A small, red item")
    val smallGreenSkuPayload = makeSkuPayload(skuGreenSmallCode, "A small, green item")
    val largeRedSkuPayload   = makeSkuPayload(skuRedLargeCode, "A small, green item")
    val largeGreenSkuPayload = makeSkuPayload(skuGreenLargeCode, "A large, green item")
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
}
