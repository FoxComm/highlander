package responses

import scala.collection.immutable.Seq

import models.location.{Country, Region}
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{CustomSerializer, Extraction}
import utils.JsonFormatters
import utils.aliases._

object PublicResponses {

  implicit val formats = JsonFormatters.DefaultFormats

  case class CountryWithRegions(country: Country, regions: Seq[Region])

  object CountryWithRegions {

    val jsonFormat = new CustomSerializer[CountryWithRegions](format ⇒
      ({
        case json: JObject ⇒
          val country = json.extract[Country]
          val regions = (json \ "regions").extract[Seq[Region]]
          CountryWithRegions(country, regions)
      }, {
        case CountryWithRegions(country, regions) ⇒
          import org.json4s.JsonDSL._
          val countryJson: Json  = Extraction.decompose(country)
          val regionsJson: Json  = Extraction.decompose(regions)
          val regionsField: Json = JField("regions", regionsJson)
          countryJson.merge(regionsField)
      }))
  }
}
