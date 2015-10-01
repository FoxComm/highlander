import akka.http.scaladsl.model.StatusCodes

import models.{Region, Country}
import models.Country._
import models.Region._
import responses.CountryWithRegions
import util.IntegrationTestBase

class PublicIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import Extensions._

  "GET /countries/:id" - {
    "lists the country and its regions along with shippable and payable flags" in {
      val response = GET(s"v1/countries/${unitedStatesId}")

      response.status must ===(StatusCodes.OK)

      val usWithRegions = response.as[CountryWithRegions]
      usWithRegions.regions.size must === (60)
      val us = usWithRegions.country
      (us.isBillable, us.isShippable) must === ((false, false))
    }
  }

  "GET /countries" - {
    "lists countries sorted" in {
      val response = GET(s"v1/countries")

      response.status mustBe StatusCodes.OK

      val countries = response.as[Seq[Country]]
      val us = countries.find(_.id === unitedStatesId).get

      us.name mustBe "United States"

      countries.head mustBe us
      countries.tail.map(_.name) mustBe sorted
    }
  }

  "GET /regions" - {
    "lists regions sorted" in {
      val response = GET(s"v1/regions")
      response.status mustBe StatusCodes.OK
      val regions = response.as[Seq[Region]]

      val us = regions.take(regularUsRegions.size)
      us.map(_.countryId) mustBe List.fill(us.size)(unitedStatesId)
      us.map(_.id).sorted mustBe regularUsRegions
      us.map(_.name) mustBe sorted

      val armed = regions.slice(regularUsRegions.size, usRegions.size)
      armed.map(_.id).sorted mustBe armedRegions
      armed.map(_.name) mustBe sorted

      val other = regions.drop(usRegions.size)
      other.map(_.name) mustBe sorted

      regions mustBe us ++ armed ++ other
    }
  }
}

