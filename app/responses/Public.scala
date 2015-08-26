package responses

import scala.collection.immutable.Seq

import models.{Country, Region}
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{CustomSerializer, Extraction, JValue}
import utils.JsonFormatters

final case class CountryWithRegions(country: Country, regions: Seq[Region])

object CountryWithRegions {
  implicit val formats = JsonFormatters.DefaultFormats

  val jsonFormat = new CustomSerializer[CountryWithRegions](format ⇒ ({
    case obj: JObject ⇒
      CountryWithRegions(obj.extract[Country], (obj \ "regions").extract[Seq[Region]])
  },{
    case CountryWithRegions(c, regions) ⇒
      import org.json4s.JsonDSL._
      Extraction.decompose(c).merge(JField("regions", Extraction.decompose(regions)): JValue)
  }))
}

