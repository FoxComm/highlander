import akka.http.scaladsl.model.StatusCodes

import com.github.tototoshi.slick.JdbcJodaSupport._
import models.{Regions, Region, Countries, Country}
import org.joda.money.CurrencyUnit
import org.json4s.JsonAST.{JField, JArray, JObject}
import services.Public.CountryWithRegions
import util.IntegrationTestBase
import slick.driver.PostgresDriver.api._
import utils._

class PublicIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import Extensions._
  import org.json4s.jackson.JsonMethods._
  import concurrent.ExecutionContext.Implicits.global

  "GET /countries/:id" - {
    "lists the country and its regions along with shippable and payable flags" in {
      val response = GET(s"v1/countries/${Country.unitedStatesId}")

      response.status must ===(StatusCodes.OK)

      val usWithRegions = response.as[CountryWithRegions]
      usWithRegions.regions.size must === (60)
      val us = usWithRegions.country
      (us.isBillable, us.isShippable) must === ((false, false))
    }
  }

  "GET /countries" - {
    "lists countries" in {
      val response = GET(s"v1/countries")

      response.status must ===(StatusCodes.OK)

      val countries = response.as[Seq[Country]]
      val us = countries.find(_.id === Country.unitedStatesId)

      us.get.name must === ("United States")
    }
  }
}

