import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ObjectFailures._
import failures.ProductFailures._
import models.Aliases.Json
import models.StoreAdmins
import models.inventory.{Sku, Skus}
import models.objects._
import models.product._
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import payloads._
import responses.ProductResponses._
import util.IntegrationTestBase
import utils.IlluminateAlgorithm
import utils.Money.Currency
import utils._
import utils.db._
import utils.db.DbResultT._
import utils.db.ExPostgresDriver.api._
import utils.db.ExPostgresDriver.jsonMethods._

class ProductIntegrationTest
  extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "GET v1/products/full/:context/:id/baked" - {
    "Return a product with multiple SKUs and variants" in new Fixture {
      val response = GET(s"v1/products/full/${context.name}/${prodForm.id}/baked")
      response.status must === (StatusCodes.OK)

      val productResponse = response.as[IlluminatedFullProductResponse.Root]
      productResponse.skus.length must === (4)
      productResponse.variants.length must === (2)
    }
  }

  trait Fixture {
    val (context, product, prodForm, prodShadow, skus, variants) = (for {
      storeAdmin  ← * <~ StoreAdmins.create(authedStoreAdmin)
      context     ← * <~ ObjectContexts.filterByName(SimpleContext.default).one.
                          mustFindOr(ObjectContextNotFound(SimpleContext.default))

      simpleProd  ← * <~ SimpleProduct(title = "Test Product",
                          description = "Test product description", image = "image.png",
                          code = "TEST")
      prodForm    ← * <~ ObjectForms.create(simpleProd.create)
      sProdShadow ← * <~ SimpleProductShadow(simpleProd)
      prodShadow  ← * <~ ObjectShadows.create(sProdShadow.create.copy(formId = prodForm.id))
      prodCommit  ← * <~ ObjectCommits.create(ObjectCommit(formId = prodForm.id,
                          shadowId = prodShadow.id))
      product     ← * <~ Products.create(Product(contextId = context.id, formId = prodForm.id,
                          shadowId = prodShadow.id, commitId = prodCommit.id))

      rawSkus     ← * <~ Seq(
        SimpleSku("SKU-RED-SMALL", "A small, red item", "http://small-red.com", 9999, Currency.USD),
        SimpleSku("SKU-RED-LARGE", "A large, red item", "http://large-red.com", 9999, Currency.USD),
        SimpleSku("SKU-GREEN-SMALL", "A small, green item", "http://small-green.com", 9999, Currency.USD),
        SimpleSku("SKU-GREEN-LARGE", "A large, green item", "http://large-green.com", 9999, Currency.USD)
      )

      skus        ← * <~ DbResultT.sequence(rawSkus.map(rawSku ⇒
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

      rawVariants ← * <~ Seq(SimpleVariant("Size"), SimpleVariant("Color"))

      variants    ← * <~ DbResultT.sequence(rawVariants.map(rawVariant ⇒
        for {
          form    ← * <~ ObjectForms.create(rawVariant.create)
          sShadow ← * <~ SimpleVariantShadow(rawVariant)
          shadow  ← * <~ ObjectShadows.create(sShadow.create.copy(formId = form.id))
          commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
          variant ← * <~ Variants.create(Variant(contextId = context.id, variantType = rawVariant.name,
                          formId = form.id, shadowId = shadow.id, commitId = commit.id))
          _       ← * <~ ObjectLinks.create(ObjectLink(leftId = prodShadow.id,
                          rightId = shadow.id, linkType = ObjectLink.ProductVariant))
        } yield variant))

    } yield (context, product, prodForm, prodShadow, skus, variants)).runTxn().futureValue.rightVal
  }
}
