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
      val response = GET(s"v1/products/full/${context.name}/${prodForm.id}/baked")
      println(response)
      response.status must === (StatusCodes.OK)

      val productResponse = response.as[IlluminatedFullProductResponse.Root]
      productResponse.skus.length must === (4)
      productResponse.variants.length must === (2)
    }
  }

  trait Fixture {
    val simpleProd = SimpleProduct(title = "Test Product", code = "TEST",
      description = "Test product description", image = "image.png")

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


    val (context, product, prodForm, prodShadow, skus, variants) = (for {
      // Create common objects.
      storeAdmin  ← * <~ StoreAdmins.create(authedStoreAdmin)
      context     ← * <~ ObjectContexts.filterByName(SimpleContext.default).one.
                          mustFindOr(ObjectContextNotFound(SimpleContext.default))

      // Create the product.
      prodForm    ← * <~ ObjectForms.create(simpleProd.create)
      sProdShadow ← * <~ SimpleProductShadow(simpleProd)
      prodShadow  ← * <~ ObjectShadows.create(sProdShadow.create.copy(formId = prodForm.id))
      prodCommit  ← * <~ ObjectCommits.create(ObjectCommit(formId = prodForm.id,
                          shadowId = prodShadow.id))
      product     ← * <~ Products.create(Product(contextId = context.id, formId = prodForm.id,
                          shadowId = prodShadow.id, commitId = prodCommit.id))

      // Create the SKUs.
      skus        ← * <~ DbResultT.sequence(simpleSkus.map(rawSku ⇒
        for {
          form    ← * <~ ObjectForms.create(rawSku.create)
          sShadow ← * <~ SimpleSkuShadow(rawSku)
          shadow  ← * <~ ObjectShadows.create(sShadow.create.copy(formId = form.id))
          commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
          sku     ← * <~ Skus.create(Sku(contextId = context.id, code = rawSku.code,
                          formId = form.id, shadowId = shadow.id, commitId = commit.id))
          _       ← * <~ ObjectLinks.create(ObjectLink(leftId = prodShadow.id,
                          rightId = shadow.id, linkType = ObjectLink.ProductSku))
        } yield sku))

      // Create the Variants and their Values.
      variantsAndValues ← * <~ DbResultT.sequence(variantsWithValues.map { scv ⇒
        for {
          form    ← * <~ ObjectForms.create(scv.v.create)
          sShadow ← * <~ SimpleVariantShadow(scv.v)
          shadow  ← * <~ ObjectShadows.create(sShadow.create.copy(formId = form.id))
          commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
          variant ← * <~ Variants.create(Variant(contextId = context.id, variantType = scv.v.name,
                          formId = form.id, shadowId = shadow.id, commitId = commit.id))
          _       ← * <~ ObjectLinks.create(ObjectLink(leftId = prodShadow.id,
                          rightId = shadow.id, linkType = ObjectLink.ProductVariant))

          values  ← * <~ DbResultT.sequence(scv.vs.map(variantValue ⇒
            for {
              form    ← * <~ ObjectForms.create(variantValue.create)
              sShadow ← * <~ SimpleVariantValueShadow(variantValue)
              shadow  ← * <~ ObjectShadows.create(sShadow.create.copy(formId = form.id))
              commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
              value   ← * <~ VariantValues.create(VariantValue(contextId = context.id,
                              formId = form.id, shadowId = shadow.id, commitId = commit.id))
              _       ← * <~ ObjectLinks.create(ObjectLink(leftId = variant.shadowId,
                              rightId = shadow.id, linkType = ObjectLink.VariantValue))
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

    } yield (context, product, prodForm, prodShadow, skus, variantsAndValues)).runTxn().futureValue.rightVal
  }
}
