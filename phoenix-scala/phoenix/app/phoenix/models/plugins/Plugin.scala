package phoenix.models.plugins

import java.time.Instant

import cats.data._
import cats.implicits._
import com.github.tminglei.slickpg._
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.Failure
import core.utils.Validation
import org.json4s.Extraction
import org.json4s.JsonAST._
import phoenix.models.account.Scope
import phoenix.models.plugins.PluginSettings._
import phoenix.payloads.PluginPayloads.RegisterPluginPayload
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import shapeless._
import slick.jdbc.PostgresProfile.api.MappedColumnType

case class Plugin(id: Int = 0,
                  scope: LTree,
                  name: String,
                  description: String,
                  isDisabled: Boolean = false,
                  version: String,
                  apiHost: Option[String], // TODO: change (apiHost, apiPort) to apiUrl @narma
                  apiPort: Option[Int],
                  settings: SettingsValues = Map.empty[String, JValue],
                  schemaSettings: SettingsSchema,
                  createdAt: Instant = Instant.now,
                  updatedAt: Option[Instant] = None,
                  deletedAt: Option[Instant] = None)
    extends FoxModel[Plugin]
    with Validation[Plugin] {
  import Validation._

  override def validate: ValidatedNel[Failure, Plugin] =
    (notEmpty(name, "name")
      |@| notEmpty(version, "version")
      |@| apiPort.fold(ok) { port ⇒
        greaterThan(port, 1, "Api port must be greater than 1")
      }
      |@| nullOrNotEmpty(apiHost, "apiHost")).map {
      case _ ⇒ this
    }

  // TODO: change me to field @narma
  def apiUrl(): Option[String] =
    for {
      host ← apiHost
      port ← apiPort
    } yield s"http://$host:$port/"
}

object Plugin {

  def fromPayload(payload: RegisterPluginPayload)(implicit au: AU): Plugin =
    Plugin(
      name = payload.name,
      version = payload.version,
      scope = Scope.current,
      description = payload.description,
      apiHost = payload.apiHost,
      apiPort = payload.apiPort,
      schemaSettings = payload.schemaSettings.getOrElse(List.empty[SettingDef])
    )
}

object PluginOrmTypeMapper {
  implicit val formats = JsonFormatters.phoenixFormats

  implicit val SettingsSchemaT = MappedColumnType
    .base[SettingsSchema, Json](v ⇒ Extraction.decompose(v), s ⇒ s.extract[SettingsSchema])

  implicit val SettingsValuesT = MappedColumnType
    .base[SettingsValues, Json](v ⇒ Extraction.decompose(v), s ⇒ s.extract[SettingsValues])

}

import phoenix.models.plugins.PluginOrmTypeMapper._

class Plugins(tag: Tag) extends FoxTable[Plugin](tag, "plugins") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope          = column[LTree]("scope")
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
     scope,
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

object Plugins extends FoxTableQuery[Plugin, Plugins](new Plugins(_)) with ReturningId[Plugin, Plugins] {

  val returningLens: Lens[Plugin, Int] = lens[Plugin].id

  object scope {
    implicit class PluginsQuerySeqConversions(q: QuerySeq) {
      def forCurrentUser()(implicit au: AU): QuerySeq =
        q.filter(Scope.current.bind @> _.scope)
    }
  }

  def findByName(name: String): QuerySeq =
    filter(_.name === name)

}
