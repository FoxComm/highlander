import akka.http.scaladsl.model.StatusCodes

import com.github.tototoshi.slick.JdbcJodaSupport._
import models.{Region, Countries, Country}
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

  "countries-and-regions" - {
    "lists countries and their regions along with shippable and payable flags on each country" in {
      val response = GET(s"v1/countries")

      response.status must ===(StatusCodes.OK)

      val total = Countries.size.result.run().futureValue
      val countriesWithRegions = parse(response.bodyText).extract[Seq[CountryWithRegions]]
      countriesWithRegions.size must === (total)
      val us = countriesWithRegions.find(_.country.name == "United States").get
      us.regions must have size (60)
    }
  }
}

