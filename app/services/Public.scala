package services

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import models.{Countries, Country, Region, Regions}
import responses.CountryWithRegions
import slick.driver.PostgresDriver.api._

object Public {
  def findCountry(countryId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Failures Xor CountryWithRegions] = {
    val query = for {
      cs ← queryCountries.filter(_.id === countryId)
      rs ← queryRegions if rs.countryId === cs.id
    } yield (cs, rs)

    db.run(query.result).flatMap { results ⇒
      results.headOption.map(_._1) match {
        case Some(c) ⇒ Result.good(CountryWithRegions(c, results.map(_._2).to[Seq]))
        case None    ⇒ Result.failure(NotFoundFailure(Country, countryId))
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
