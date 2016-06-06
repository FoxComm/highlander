import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ObjectFailures.ObjectContextNotFound
import models.objects.ObjectContexts
import models.product.SimpleContext
import org.json4s.JsonDSL._
import payloads.VariantPayloads.{CreateVariantPayload, CreateVariantValuePayload}
import responses.VariantResponses.IlluminatedVariantResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._

class VariantIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST v1/variants/:context" - {
    "Creates a variant successfully" in new Fixture {
      val response = POST(s"v1/variants/${context.name}", createVariantPayload)
      response.status must ===(StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must ===(0)

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must ===("Color")
    }

    "Creates a variant with a value successfully" in new Fixture {
      val payload  = createVariantPayload.copy(values = Some(Seq(createVariantValuePayload)))
      val response = POST(s"v1/variants/${context.name}", payload)
      response.status must ===(StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must ===(1)
      variantResponse.values.head.name must ===("Red")
      variantResponse.values.head.swatch must ===(Some("ff0000"))

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must ===("Color")
    }
  }

  "POST v1/variants/:context/:id/values" - {
    "Creates a variant value successfully" in new Fixture {
      val response = POST(s"v1/variants/${context.name}", createVariantPayload)
      response.status must ===(StatusCodes.OK)
      val variantResponse = response.as[IlluminatedVariantResponse.Root]

      val response2 = POST(s"v1/variants/${context.name}/${variantResponse.id}/values",
                           createVariantValuePayload)
      response2.status must ===(StatusCodes.OK)
      val valueResponse = response2.as[IlluminatedVariantValueResponse.Root]
      valueResponse.swatch must ===(Some("ff0000"))
    }
  }

  trait Fixture {
    val createVariantPayload = CreateVariantPayload(
        attributes = Map("name" → (("t" → "string") ~ ("v" → "Color"))), values = None)

    val createVariantValuePayload = CreateVariantValuePayload(
        name = "Red", swatch = Some("ff0000"))

    val context = (for {
      context ← * <~ ObjectContexts
                 .filterByName(SimpleContext.default)
                 .mustFindOneOr(ObjectContextNotFound(SimpleContext.default))
    } yield context).runTxn().futureValue.rightVal
  }
}
