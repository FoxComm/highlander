package anthill.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{ Matchers, WordSpec }

class PublicRoutesSpec extends WordSpec with Matchers with ScalatestRouteTest {

  "PublicRoutes" should {
    "GET `/public/ping`" in {
      Get("/public/ping") ~> PublicRoutes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "GET `/public/prod-prod/1`" in {
      Get("/public/prod-prod/1") ~> PublicRoutes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "GET `/public/prod-prod/full/1`" in {
      Get("/public/prod-prod/full/1") ~> PublicRoutes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "GET `/public/cust-prod/1`" in {
      Get("/public/cust-prod/1") ~> PublicRoutes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "GET `/public/cust-prod/full/1`" in {
      Get("/public/cust-prod/full/1") ~> PublicRoutes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

}
