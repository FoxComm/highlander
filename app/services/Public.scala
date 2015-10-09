package services

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

import models.Country._
import models.Region._
import models.{Countries, Country, Region, Regions}
import responses.CountryWithRegions
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult

object Public {

  def findCountry(countryId: Int)(implicit ec: ExecutionContext, db: Database): Result[CountryWithRegions] = {
    Countries._findById(countryId).extract.selectOne { country ⇒
      DbResult.fromDbio(Regions.filter(_.countryId === countryId).result.map { rs ⇒
        CountryWithRegions(country, rs.to[Seq])
      })
    }
  }

  def countries(implicit ec: ExecutionContext, db: Database): Future[Seq[Country]] =
    db.run(Countries.result).map { countries ⇒
      val usa = countries.filter(_.id == unitedStatesId)
      val othersSorted = countries.filterNot(_.id == unitedStatesId).sortBy(_.name)
      (usa ++ othersSorted).to[Seq]
    }

  def regions(implicit ec: ExecutionContext, db: Database): Future[Seq[Region]] =
    db.run(Regions.result).map { regions ⇒
      (regions.filter(r ⇒ regularUsRegions.contains(r.id)).sortBy(_.name)
        ++ regions.filter(r ⇒ armedRegions.contains(r.id)).sortBy(_.name)
        ++ regions.filterNot(r ⇒ usRegions.contains(r.id)).sortBy(_.name)).to[Seq]
    }
}
