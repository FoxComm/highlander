package phoenix.services

import core.db._
import phoenix.failures.AddressFailures.NoRegionFound
import phoenix.models.location.Country._
import phoenix.models.location.Region._
import phoenix.models.location._
import phoenix.responses.PublicResponses.CountryWithRegions
import slick.jdbc.PostgresProfile.api._

import scala.collection.immutable.Seq
import scala.concurrent.Future

object PublicService {

  def findCountry(countryId: Int)(implicit ec: EC, db: DB): DbResultT[CountryWithRegions] =
    for {
      country ← * <~ Countries.mustFindById404(countryId)
      regions ← * <~ Regions.filter(_.countryId === country.id).result
    } yield CountryWithRegions(country, sortRegions(regions.to[Seq]))

  def listCountries(implicit ec: EC, db: DB): Future[Seq[Country]] =
    db.run(Countries.result).map { countries ⇒
      val usa          = countries.filter(_.id == unitedStatesId)
      val othersSorted = countries.filterNot(_.id == unitedStatesId).sortBy(_.name)
      (usa ++ othersSorted).to[Seq]
    }

  def listRegions(implicit ec: EC, db: DB): Future[Seq[Region]] =
    db.run(Regions.result.map(rs ⇒ sortRegions(rs.to[Seq])))

  def findRegionByShortName(regionShortName: String)(implicit ec: EC, db: DB): DbResultT[Region] =
    for {
      region ← * <~ Regions
                .findOneByShortName(regionShortName)
                .mustFindOneOr(NoRegionFound(regionShortName))
    } yield region

  private def sortRegions(regions: Seq[Region]): Seq[Region] =
    regions.filter(r ⇒ regularUsRegions.contains(r.id)).sortBy(_.name) ++
      regions.filter(r ⇒ armedRegions.contains(r.id)).sortBy(_.name) ++
      regions.filterNot(r ⇒ usRegions.contains(r.id)).sortBy(_.name)
}
