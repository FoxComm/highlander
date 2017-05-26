package phoenix.models.catalog

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import shapeless._

import core.db._
import core.db.ExPostgresDriver.api._

case class Catalog(id: Int = 0,
                   scope: LTree,
                   name: String,
                   countryId: Int,
                   defaultLanguage: String,
                   createdAt: Instant = Instant.now,
                   updatedAt: Instant = Instant.now)
    extends FoxModel[Catalog]

class Catalogs(tag: Tag) extends FoxTable[Catalog](tag, "catalogs") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope           = column[LTree]("scope")
  def name            = column[String]("name")
  def countryId       = column[Int]("country_id")
  def defaultLanguage = column[String]("default_language")
  def createdAt       = column[Instant]("created_at")
  def updatedAt       = column[Instant]("updated_at")

  def * =
    (id, scope, name, countryId, defaultLanguage, createdAt, updatedAt) <> ((Catalog.apply _).tupled, Catalog.unapply)
}

object Catalogs
    extends FoxTableQuery[Catalog, Catalogs](new Catalogs(_))
    with ReturningId[Catalog, Catalogs] {
  val returningLens: Lens[Catalog, Int] = lens[Catalog].id
}
