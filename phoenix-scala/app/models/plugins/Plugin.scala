package models.plugins

import java.time.Instant

import cats.data._
import failures.Failure
import org.json4s.JsonAST._
import payloads.PluginPayloads.RegisterPluginPayload
import shapeless._
import utils.{JsonFormatters, Validation}
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import cats.implicits._
import PluginSettings._
import com.github.tminglei.slickpg.utils.SimpleArrayUtils
import org.json4s.jackson.JsonMethods._
import slick.driver.PostgresDriver.api.MappedColumnType
import org.json4s.Extraction

case class Plugin(id: Int = 0,
                  name: String,
                  description: String,
                  isDisabled: Boolean = false,
                  version: String,
                  apiHost: String,
                  apiPort: Int,
                  settings: SettingsValues = Map.empty[String, JValue],
                  schemaSettings: SettingsSchema,
                  createdAt: Instant = Instant.now,
                  updatedAt: Option[Instant] = None,
                  deletedAt: Option[Instant] = None)
    extends FoxModel[Plugin]
    with Validation[Plugin] {
  import Validation._

  override def validate: ValidatedNel[Failure, Plugin] = {

    (notEmpty(name, "name")
      |@| notEmpty(version, "version")
      |@| greaterThan(apiPort, 1, "Api port must be greater than 1")
      |@| notEmpty(apiHost, "apiHost")).map {
      case _ ⇒ this
    }
  }
}

object Plugin {

  def fromPayload(payload: RegisterPluginPayload): Plugin = {
    Plugin(name = payload.name,
           version = payload.version,
           description = payload.description,
           apiHost = payload.apiHost,
           apiPort = payload.apiPort,
           schemaSettings = payload.schemaSettings.getOrElse(List.empty[SettingDef]))
  }
}

object PluginOrmTypeMapper {
  implicit val formats = JsonFormatters.phoenixFormats

  implicit val SettingsSchemaT = MappedColumnType
    .base[SettingsSchema, Json](v ⇒ Extraction.decompose(v), s ⇒ s.extract[SettingsSchema])

  implicit val SettingsValuesT = MappedColumnType
    .base[SettingsValues, Json](v ⇒ Extraction.decompose(v), s ⇒ s.extract[SettingsValues])

}

import PluginOrmTypeMapper._

class Plugins(tag: Tag) extends FoxTable[Plugin](tag, "plugins") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name           = column[String]("name")
  def description    = column[String]("description")
  def isDisabled     = column[Boolean]("is_disabled")
  def version        = column[String]("version")
  def apiHost        = column[String]("api_host")
  def apiPort        = column[Int]("api_port")
  def settings       = column[SettingsValues]("settings")
  def schemaSettings = column[SettingsSchema]("schema_settings")

  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Option[Instant]]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id,
     name,
     description,
     isDisabled,
     version,
     apiHost,
     apiPort,
     settings,
     schemaSettings,
     createdAt,
     updatedAt,
     deletedAt) <>
      ((Plugin.apply _).tupled, Plugin.unapply)
}

object Plugins
    extends FoxTableQuery[Plugin, Plugins](new Plugins(_))
    with ReturningId[Plugin, Plugins] {

  val returningLens: Lens[Plugin, Int] = lens[Plugin].id

  def findByName(name: String): DBIO[Option[Plugin]] = {
    filter(_.name === name).one
  }
}
