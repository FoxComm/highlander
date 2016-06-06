import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import failures.ObjectFailures.ObjectContextNotFound
import models.objects.ObjectContexts
import models.product.SimpleContext
import org.json4s.JsonDSL._
import payloads.VariantPayloads.CreateVariantPayload
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._

class VariantIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST v1/variants/:context" - {
    "Creates a variant successfully" in new Fixture {
      val response = POST(s"v1/variants/${context.name}", createVariantPayload)
      println(response)
      response.status must ===(StatusCodes.OK)
    }
  }

  trait Fixture {
    val createVariantPayload = CreateVariantPayload(
        Map("name" → (("t" → "string") ~ ("v" → "Color"))))

    val context = (for {
      context ← * <~ ObjectContexts
                 .filterByName(SimpleContext.default)
                 .mustFindOneOr(ObjectContextNotFound(SimpleContext.default))
    } yield context).runTxn().futureValue.rightVal
  }
}
