package models.plugins

import cats.data._
import cats.implicits._
import failures.Failure
import io.circe.JsonObject
import java.time.Instant
import models.plugins.PluginSettings._
import payloads.PluginPayloads.RegisterPluginPayload
import shapeless._
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.json._
import utils.json.codecs._

case class Plugin(id: Int = 0,
                  name: String,
                  description: String,
                  isDisabled: Boolean = false,
                  version: String,
                  apiHost: Option[String], // TODO: change (apiHost, apiPort) to apiUrl @narma
                  apiPort: Option[Int],
                  settings: SettingsValues = JsonObject.empty,
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
          |@| apiPort.fold(ok) { port ⇒
            greaterThan(port, 1, "Api port must be greater than 1")
          }
          |@| nullOrNotEmpty(apiHost, "apiHost")).map {
      case _ ⇒ this
    }
  }

  // TODO: change me to field @narma
  def apiUrl(): Option[String] =
    for {
      host ← apiHost
      port ← apiPort
    } yield s"http://$host:$port/"
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
  implicit val SettingsSchemaT: BaseColumnType[SettingsSchema] = dbJsonColumn[SettingsSchema]

  implicit val SettingsValuesT: BaseColumnType[SettingsValues] = dbJsonColumn[SettingsValues]
}

import models.plugins.PluginOrmTypeMapper._

class Plugins(tag: Tag) extends FoxTable[Plugin](tag, "plugins") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name           = column[String]("name")
  def description    = column[String]("description")
  def isDisabled     = column[Boolean]("is_disabled")
  def version        = column[String]("version")
  def apiHost        = column[Option[String]]("api_host")
  def apiPort        = column[Option[Int]]("api_port")
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
