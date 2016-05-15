import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ObjectFailures._
import models.StoreAdmins
import models.inventory.{Sku, Skus}
import models.objects._
import models.product._
import responses.ProductResponses._
import util.IntegrationTestBase
import utils.Money.Currency
import utils.db._
import utils.db.DbResultT._

class ProductIntegrationTest
  extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "GET v1/products/full/:context/:id/baked" - {
    "Return a product with multiple SKUs and variants" in new Fixture {
      val response = GET(s"v1/products/full/${context.name}/${product.formId}/baked")
      response.status must === (StatusCodes.OK)

      val productResponse = response.as[IlluminatedFullProductResponse.Root]
      productResponse.skus.length must === (4)
      productResponse.variants.length must === (2)

      val variantMap = productResponse.variantMap.extract[Map[String, Seq[Int]]]
      variantMap.size must === (4)

      val varOne :: varTwo :: Nil = productResponse.variants
      varOne.values.length must === (2)
      varTwo.values.length must === (2)
    }
  }

  trait Fixture {
    val simpleProd = SimpleProductData(title = "Test Product", code = "TEST",
      description = "Test product description", image = "image.png", price = 5999)

    val simpleSkus = Seq(
        SimpleSku("SKU-RED-SMALL", "A small, red item", "http://small-red.com", 9999, Currency.USD),
        SimpleSku("SKU-RED-LARGE", "A large, red item", "http://large-red.com", 9999, Currency.USD),
        SimpleSku("SKU-GREEN-SMALL", "A small, green item", "http://small-green.com", 9999, Currency.USD),
        SimpleSku("SKU-GREEN-LARGE", "A large, green item", "http://large-green.com", 9999, Currency.USD))

    val variantsWithValues = Seq(
      SimpleCompleteVariant(SimpleVariant("Size"),
        Seq(SimpleVariantValue("small", ""), SimpleVariantValue("large", ""))),
      SimpleCompleteVariant(SimpleVariant("Color"),
        Seq(SimpleVariantValue("red", "ff0000"), SimpleVariantValue("green", "00ff00"))))

    val skuValueMapping: Seq[(String, String, String)] = Seq(
      ("SKU-RED-SMALL", "red", "small"),
      ("SKU-RED-LARGE", "red", "large"),
      ("SKU-GREEN-SMALL", "green", "small"),
      ("SKU-GREEN-LARGE", "green", "large"))


    val (context, product, skus, variants) = (for {
      // Create common objects.
      storeAdmin  ← * <~ StoreAdmins.create(authedStoreAdmin)
      context     ← * <~ ObjectContexts.filterByName(SimpleContext.default).one.
                          mustFindOr(ObjectContextNotFound(SimpleContext.default))

      // Create the SKUs.
      skus        ← * <~ Mvp.insertSkus(context.id, simpleSkus)

      // Create the product.
      product     ← * <~ Mvp.insertProductWithExistingSkus(context.id, simpleProd, skus)

      // Create the Variants and their Values.
      variantsAndValues ← * <~ DbResultT.sequence(variantsWithValues.map { scv ⇒
        for {
          variant ← * <~ Mvp.insertVariant(context.id, scv.v, product.shadowId)
          values  ← * <~ DbResultT.sequence(scv.vs.map(variantValue ⇒
            for {
              value ← * <~ Mvp.insertVariantValue(context.id, variantValue, variant.shadowId)
          } yield (variantValue.name, value)))
        } yield (variant, values) } )

      variants      ← * <~ variantsAndValues.map { case (variant, values) ⇒ variant }
      variantValues ← * <~ variantsAndValues.foldLeft(Seq.empty[(String, VariantValue)]) { (acc, item) ⇒
        val nameValuePair = item._2
        acc ++ nameValuePair
      }

      // Map the SKUs to the Variant Values
      skuMap ← * <~ DbResultT.sequence(skuValueMapping.map { case (code, colorName, sizeName) ⇒
        val selectedSku = skus.filter(_.code == code).head
        val colorValue = variantValues.filter { case (n, _) ⇒ n == colorName }.map { case (_, v) ⇒ v }.head
        val sizeValue = variantValues.filter { case (n, _) ⇒ n == sizeName }.map { case (_, v) ⇒ v }.head

        for {
          colorLink ← * <~ ObjectLinks.create(ObjectLink(leftId = selectedSku.shadowId,
            rightId = colorValue.shadowId, linkType = ObjectLink.SkuVariantValue))
          sizeLink  ← * <~ ObjectLinks.create(ObjectLink(leftId = selectedSku.shadowId,
            rightId = sizeValue.shadowId, linkType = ObjectLink.SkuVariantValue))
        } yield (colorLink, sizeLink)
      })

    } yield (context, product, skus, variantsAndValues)).runTxn().futureValue.rightVal
  }
}
