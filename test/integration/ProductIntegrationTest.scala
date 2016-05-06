import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ImageFailures._
import failures.ObjectFailures._
import failures.ProductFailures._
import models.Aliases.Json
import models.image._
import models.objects._
import models.product._
import models.StoreAdmins
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import payloads._
import responses.ImageResponses.AlbumResponse
import util.IntegrationTestBase
import util.SlickSupport.implicits._
import utils.IlluminateAlgorithm
import utils.Money.Currency
import utils._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._
import utils.db.ExPostgresDriver.api._
import utils.db.ExPostgresDriver.jsonMethods._

class ProductIntegrationTest 
  extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "Product Tests" - {
    "POST v1/products/:context/:id/albums" - {
      "Creates a new album on an existing product" in new Fixture {
        val payload = AlbumPayload(name = "Simple Album",
          images = Some(Seq(ImagePayload(src = "http://test.com/test.jpg"))))
        val response = POST(s"v1/products/${context.name}/${prodForm.id}/albums", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.images.length must === (1)
      }
    }
  }

  trait Fixture {
    val (context, product, prodForm, prodShadow) = (for {
      storeAdmin  ← * <~ StoreAdmins.create(authedStoreAdmin)
      context     ← * <~ ObjectContexts.filterByName(SimpleContext.default).one.
                          mustFindOr(ObjectContextNotFound(SimpleContext.default))

      simpleSku   ← * <~ SimpleSku("SKU-TEST", "Test SKU", "http://poop/", 9999, Currency.USD)
      sSkuShadow  ← * <~ SimpleSkuShadow(simpleSku)

      simpleProd  ← * <~ SimpleProduct(title = "Test Product",
                          description = "Test product description", image = "image.png",
                          code = simpleSku.code)
      prodForm    ← * <~ ObjectForms.create(simpleProd.create)
      sProdShadow ← * <~ SimpleProductShadow(simpleProd)
      prodShadow  ← * <~ ObjectShadows.create(sProdShadow.create.copy(formId = prodForm.id))
      prodCommit  ← * <~ ObjectCommits.create(ObjectCommit(formId = prodForm.id,
                          shadowId = prodShadow.id))
      product     ← * <~ Products.create(Product(contextId = context.id, formId = prodForm.id,
                          shadowId = prodShadow.id, commitId = prodCommit.id))
    } yield (context, product, prodForm, prodShadow)).runTxn().futureValue.rightVal
  }
}
