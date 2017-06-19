package anthill.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import anthill.payloads.PurchaseEventPayload
import anthill.util.JsonSupport
import org.scalatest.{Matchers, WordSpec}

class PrivateRoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {

  "PrivateRoutes" should {
    "POST `/private/prod-prod/train`" in {
      Post("/private/prod-prod/train", PurchaseEventPayload(1, List(1, 2, 3), Some(1))) ~> PrivateRoutes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }
}
