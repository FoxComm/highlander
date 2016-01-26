package services

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

import models.Country._
import models.Region._
import models.{Countries, Country, Region, Regions}
import responses.CountryWithRegions
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

object Public {

  def findCountry(countryId: Int)(implicit ec: ExecutionContext, db: Database): Result[CountryWithRegions] = (for {
    country ← * <~ Countries.mustFindById(countryId)
    regions ← * <~ Regions.filter(_.countryId === country.id).result.toXor
  } yield CountryWithRegions(country, sortRegions(regions.to[Seq]))).run()

  def countries(implicit ec: ExecutionContext, db: Database): Future[Seq[Country]] =
    db.run(Countries.result).map { countries ⇒
      val usa = countries.filter(_.id == unitedStatesId)
      val othersSorted = countries.filterNot(_.id == unitedStatesId).sortBy(_.name)
      (usa ++ othersSorted).to[Seq]
    }

  def regions(implicit ec: ExecutionContext, db: Database): Future[Seq[Region]] =
    db.run(Regions.result.map(rs ⇒ sortRegions(rs.to[Seq])))

  private def sortRegions(regions: Seq[Region]): Seq[Region] = {
    regions.filter(r ⇒ regularUsRegions.contains(r.id)).sortBy(_.name) ++
    regions.filter(r ⇒ armedRegions.contains(r.id)).sortBy(_.name) ++
    regions.filterNot(r ⇒ usRegions.contains(r.id)).sortBy(_.name)
  }
}
