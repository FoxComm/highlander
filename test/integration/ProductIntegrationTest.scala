import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ObjectFailures._
import models.StoreAdmins
import models.inventory.{Sku, Skus}
import models.objects._
import models.product._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import payloads.ProductPayloads._
import responses.ProductResponses._
import util.IntegrationTestBase
import utils.Money.Currency
import utils.db._
import utils.db.DbResultT._

class ProductIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST v1/products/:context" - {
    "Creates a product successfully" in new Fixture {
      val descriptionJson = ("t" -> "string") ~ ("v" -> "Product description")
      val attrMap         = Map("description" -> descriptionJson)
      val payload         = CreateProductPayload("Product name", attrMap)

      val response = POST(s"v1/products/${context.name}", payload)
      response.status must ===(StatusCodes.OK)
    }
  }

  "GET v1/products/full/:context/:id/baked" - {
    "Return a product with multiple SKUs and variants" in new Fixture {
      val response = GET(s"v1/products/full/${context.name}/${product.formId}/baked")
      response.status must ===(StatusCodes.OK)

      val productResponse = response.as[IlluminatedFullProductResponse.Root]
      productResponse.skus.length must ===(4)
      productResponse.variants.length must ===(2)

      val variantMap = productResponse.variantMap.extract[Map[String, Seq[Int]]]
      variantMap.size must ===(4)

      val varOne :: varTwo :: Nil = productResponse.variants
      varOne.values.length must ===(2)
      varTwo.values.length must ===(2)
    }
  }

  trait Fixture {
    val simpleProd = SimpleProductData(title = "Test Product",
                                       code = "TEST",
                                       description = "Test product description",
                                       image = "image.png",
                                       price = 5999)

    val simpleSkus = Seq(SimpleSku("SKU-RED-SMALL",
                                   "A small, red item",
                                   "http://small-red.com",
                                   9999,
                                   Currency.USD),
                         SimpleSku("SKU-RED-LARGE",
                                   "A large, red item",
                                   "http://large-red.com",
                                   9999,
                                   Currency.USD),
                         SimpleSku("SKU-GREEN-SMALL",
                                   "A small, green item",
                                   "http://small-green.com",
                                   9999,
                                   Currency.USD),
                         SimpleSku("SKU-GREEN-LARGE",
                                   "A large, green item",
                                   "http://large-green.com",
                                   9999,
                                   Currency.USD))

    val variantsWithValues = Seq(
        SimpleCompleteVariant(
            SimpleVariant("Size"),
            Seq(SimpleVariantValue("small", ""), SimpleVariantValue("large", ""))),
        SimpleCompleteVariant(SimpleVariant("Color"),
                              Seq(SimpleVariantValue("red", "ff0000"),
                                  SimpleVariantValue("green", "00ff00"))))

    val skuValueMapping: Seq[(String, String, String)] = Seq(("SKU-RED-SMALL", "red", "small"),
                                                             ("SKU-RED-LARGE", "red", "large"),
                                                             ("SKU-GREEN-SMALL", "green", "small"),
                                                             ("SKU-GREEN-LARGE", "green", "large"))

    val (context, product, skus, variants) = (for {
      // Create common objects.
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
      context ← * <~ ObjectContexts
                 .filterByName(SimpleContext.default)
                 .mustFindOneOr(ObjectContextNotFound(SimpleContext.default))

      // Create the SKUs.
      skus ← * <~ Mvp.insertSkus(context.id, simpleSkus)

      // Create the product.
      product ← * <~ Mvp.insertProductWithExistingSkus(context.id, simpleProd, skus)

      // Create the Variants and their Values.
      variantsAndValues ← * <~ DbResultT.sequence(variantsWithValues.map { scv ⇒
                           Mvp.insertVariantWithValues(context.id, product.shadowId, scv)
                         })

      variants ← * <~ variantsAndValues.map(_.variant)
      variantValues ← * <~ variantsAndValues.foldLeft(Seq.empty[SimpleVariantValueData]) {
                       (acc, item) ⇒
                         acc ++ item.variantValues
                     }

      // Map the SKUs to the Variant Values
      skuMap ← * <~ DbResultT.sequence(skuValueMapping.map {
                case (code, colorName, sizeName) ⇒
                  val selectedSku = skus.filter(_.code == code).head
                  val colorValue  = variantValues.filter(_.name == colorName).head
                  val sizeValue   = variantValues.filter(_.name == sizeName).head

                  for {
                    colorLink ← * <~ ObjectLinks.create(
                                   ObjectLink(leftId = selectedSku.shadowId,
                                              rightId = colorValue.shadowId,
                                              linkType = ObjectLink.SkuVariantValue))
                    sizeLink ← * <~ ObjectLinks.create(
                                  ObjectLink(leftId = selectedSku.shadowId,
                                             rightId = sizeValue.shadowId,
                                             linkType = ObjectLink.SkuVariantValue))
                  } yield (colorLink, sizeLink)
              })
    } yield (context, product, skus, variantsAndValues)).runTxn().futureValue.rightVal
  }
}
