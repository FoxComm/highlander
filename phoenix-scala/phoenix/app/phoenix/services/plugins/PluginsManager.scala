package phoenix.services.plugins

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.{GeneralFailure, NotFoundFailure404}
import dispatch.{Http, as, host, url ⇒ request}
import phoenix.models.account.Scope
import org.json4s.Formats
import org.json4s.jackson.JsonMethods._
import phoenix.models.plugins.PluginSettings.SettingsValues._
import phoenix.models.plugins.PluginSettings.{SettingsSchema, SettingsValues}
import phoenix.models.plugins._
import phoenix.models.plugins.Plugins.scope._
import phoenix.payloads.PluginPayloads._
import phoenix.responses.plugins.PluginCommonResponses._
import phoenix.utils.aliases._

import scala.concurrent.Future

object PluginsManager extends LazyLogging {

  private def getSchemaSettings(plugin: Plugin,
                                payload: RegisterPluginPayload,
                                foundOrCreated: FoundOrCreated)(implicit ec: EC): DbResultT[SettingsSchema] =
    payload.schemaSettings.fold {
      plugin
        .apiUrl()
        .fold(DbResultT.failure[SettingsSchema](GeneralFailure("settingsSchema or apiUrl should be " +
          "present"))) { apiUrl ⇒
          val req = host(apiUrl) / "_settings" / "schema"
          DbResultT.fromF(DBIO.from(Http(req OK as.json4s.Json).map(_.extract[SettingsSchema])))
        }
    }(_.pure[DbResultT])

  def uploadNewSettingsToPlugin(plugin: Plugin)(implicit ec: EC, formats: Formats): Future[String] =
    plugin.apiUrl().fold(Future.successful("")) { apiUrl ⇒
      val rawReq = host(apiUrl) / "_settings" / "upload"
      val body   = compact(render(plugin.settings.toJson))
      val req    = rawReq.setContentType("application/json", "UTF-8") << body
      logger.info(s"Updating plugin ${plugin.name} at ${plugin.apiHost}:${plugin.apiPort}: $body")
      val resp = Http(req.POST OK as.String)
      resp.map { respBody ⇒
        logger.info(s"Plugin Response: $respBody")
        respBody
      }
    }

  private def updatePluginInfo(plugin: Plugin, schema: SettingsSchema, payload: RegisterPluginPayload)(
      implicit ec: EC): DbResultT[Plugin] = {
    val newSettings = schema
      .filterNot { s ⇒
        plugin.settings.contains(s.name)
      }
      .foldLeft(plugin.settings) { (settings, schemaSetting) ⇒
        settings + (schemaSetting.name → schemaSetting.default)
      }
    Plugins.update(
      plugin,
      plugin.copy(settings = newSettings,
                  schemaSettings = schema,
                  apiHost = payload.apiHost,
                  apiPort = payload.apiPort,
                  version = payload.version,
                  description = payload.description)
    )
  }

  private def updatePlugin(plugin: Plugin, payload: RegisterPluginPayload, foundOrCreated: FoundOrCreated)(
      implicit ec: EC): DbResultT[Plugin] =
    for {
      schema ← * <~ getSchemaSettings(plugin, payload, foundOrCreated)
      plugin ← * <~ updatePluginInfo(plugin, schema, payload)
    } yield plugin

  def listPlugins()(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[ListPluginsAnswer] =
    for {
      plugins ← * <~ Plugins.forCurrentUser.result
    } yield plugins.map(PluginInfo.fromPlugin)

  def registerPlugin(
      payload: RegisterPluginPayload)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[RegisterAnswer] = {
    val pluginT = for {
      result ← * <~ Plugins
                .findByName(payload.name)
                .forCurrentUser
                .one
                .findOrCreateExtended(Plugins.create(Plugin.fromPayload(payload)))
      (dbPlugin, foundOrCreated) = result
      plugin ← * <~ updatePlugin(dbPlugin, payload, foundOrCreated)

    } yield (plugin, foundOrCreated)

    pluginT.map {
      case (plugin, foundOrCreated) ⇒
        uploadNewSettingsToPlugin(plugin)
        buildRegister(plugin, foundOrCreated)
    }
  }

  def updateSettings(
      name: String,
      payload: UpdateSettingsPayload)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[SettingsUpdated] = {
    val updated = for {
      plugin ← * <~ Plugins
                .findByName(name)
                .forCurrentUser
                .mustFindOneOr(NotFoundFailure404(Plugin, name))
      newSettings = plugin.settings merge payload.settings
      updated ← * <~ Plugins.update(plugin, plugin.copy(settings = newSettings))
    } yield updated

    updated.map { p ⇒
      uploadNewSettingsToPlugin(p) onFailure {
        case e ⇒
          logger.error(s"Can't upload new settings to Plugin $name: ${e.getMessage}")
      }
      SettingsUpdated(p.settings)
    }
  }

  def listSettings(name: String)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[SettingsValues] =
    for {
      plugin ← * <~ Plugins
                .findByName(name)
                .forCurrentUser
                .mustFindOneOr(NotFoundFailure404(Plugin, name))
    } yield plugin.settings

  def getSettingsWithSchema(
      name: String)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[PluginSettingsResponse] =
    for {
      plugin ← * <~ Plugins
                .findByName(name)
                .forCurrentUser
                .mustFindOneOr(NotFoundFailure404(Plugin, name))
    } yield PluginSettingsResponse.fromPlugin(plugin)

}
