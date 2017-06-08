package phoenix.models.catalog

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import shapeless._
import core.db._
import core.db.ExPostgresDriver.api._
import phoenix.models.location._
import phoenix.payloads.CatalogPayloads._

case class Catalog(id: Int,
                   scope: LTree,
                   name: String,
                   site: Option[String],
                   countryId: Int,
                   defaultLanguage: String,
                   createdAt: Instant,
                   updatedAt: Instant)
    extends FoxModel[Catalog]

object Catalog {
  def build(payload: CreateCatalogPayload, scope: LTree): Catalog =
    Catalog(
      id = 0,
      scope = scope,
      name = payload.name,
      site = payload.site,
      countryId = payload.countryId,
      defaultLanguage = payload.defaultLanguage,
      createdAt = Instant.now,
      updatedAt = Instant.now
    )

  def build(existing: Catalog, payload: UpdateCatalogPayload): Catalog = {
    val site = payload.site match {
      case Some(site) if site.trim.nonEmpty ⇒ payload.site
      case Some(site) if site.trim.isEmpty  ⇒ None
      case None                             ⇒ existing.site
    }

    existing.copy(
      name = payload.name.getOrElse(existing.name),
      site = site,
      countryId = payload.countryId.getOrElse(existing.countryId),
      defaultLanguage = payload.defaultLanguage.getOrElse(existing.defaultLanguage),
      updatedAt = Instant.now
    )
  }
}

class Catalogs(tag: Tag) extends FoxTable[Catalog](tag, "catalogs") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope           = column[LTree]("scope")
  def name            = column[String]("name")
  def site            = column[Option[String]]("site")
  def countryId       = column[Int]("country_id")
  def defaultLanguage = column[String]("default_language")
  def createdAt       = column[Instant]("created_at")
  def updatedAt       = column[Instant]("updated_at")

  def * =
    (id, scope, name, site, countryId, defaultLanguage, createdAt, updatedAt) <> ((Catalog.apply _).tupled, Catalog.unapply)
}

object Catalogs
    extends FoxTableQuery[Catalog, Catalogs](new Catalogs(_))
    with ReturningId[Catalog, Catalogs] {
  val returningLens: Lens[Catalog, Int] = lens[Catalog].id

  def filterWithCountry(id: Int): Query[(Catalogs, Countries), (Catalog, Country), Seq] =
    for {
      catalog ← Catalogs.filter(_.id === id)
      country ← Countries if country.id === catalog.countryId
    } yield (catalog, country)
}
