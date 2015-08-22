package services

import scala.concurrent.{ExecutionContext, Future}

import models.{Countries, Country, Region, Regions}
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{CustomSerializer, DefaultFormats, Extraction, JValue}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object Public {
  final case class CountryWithRegions(country: Country, regions: Seq[Region])

  object CountryWithRegions {
    implicit val formats = DefaultFormats + utils.Money.jsonFormat

    val jsonFormat = new CustomSerializer[CountryWithRegions](format ⇒ ({
      case obj @ JObject(_) ⇒
        CountryWithRegions(obj.extract[Country], (obj \ "regions").extract[Seq[Region]])
    },{
      case CountryWithRegions(c, regions) ⇒
        import org.json4s.JsonDSL._
        Extraction.decompose(c).merge(JField("regions", Extraction.decompose(regions)): JValue)
    }))
  }

  def listCountries(implicit ec: ExecutionContext, db: Database): Future[Seq[CountryWithRegions]] = {
    val q = for {
      countries ← Countries.sortBy(_.id.asc).result
      regions ← Regions.sortBy(_.countryId.asc).result
    } yield (countries, regions)

    db.run(q).map { case (countries, regions) ⇒
      countries.map { c ⇒ CountryWithRegions(c, regions.filter(_.countryId == c.id)) }
    }
  }
}
