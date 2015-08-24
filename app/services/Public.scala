package services

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

import models.{Countries, Country, Region, Regions}
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.{CustomSerializer, DefaultFormats, Extraction, JValue}
import org.scalactic.{Bad, Good, Or}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object Public {
  final case class CountryWithRegions(country: Country, regions: Seq[Region])

  object CountryWithRegions {
    implicit val formats = DefaultFormats + utils.Money.jsonFormat

    val jsonFormat = new CustomSerializer[CountryWithRegions](format ⇒ ({
      case obj: JObject ⇒
        CountryWithRegions(obj.extract[Country], (obj \ "regions").extract[Seq[Region]])
    },{
      case CountryWithRegions(c, regions) ⇒
        import org.json4s.JsonDSL._
        Extraction.decompose(c).merge(JField("regions", Extraction.decompose(regions)): JValue)
    }))
  }

  def findCountry(countryId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[CountryWithRegions Or Failures] = {
    val query = for {
      cs ← queryCountries.filter(_.id === countryId)
      rs ← queryRegions if rs.countryId === cs.id
    } yield (cs, rs)

    db.run(query.result).map { results ⇒
      results.headOption.map(_._1) match {
        case None     ⇒ Bad(NotFoundFailure(Country, countryId).single)
        case Some(c)  ⇒ Good(CountryWithRegions(c, results.map(_._2).to[Seq]))
      }
    }
  }

  def countries(implicit ec: ExecutionContext, db: Database): Future[Seq[Country]] =
    db.run(queryCountries.result).map(_.to[Seq])

  private def queryCountries(implicit ec: ExecutionContext, db: Database): Query[Countries, Country, scala.Seq] =
    Countries.sortBy(_.id.asc)

  private def queryRegions(implicit ec: ExecutionContext, db: Database): Query[Regions, Region, scala.Seq] =
    Regions.sortBy(_.id.asc)
}
