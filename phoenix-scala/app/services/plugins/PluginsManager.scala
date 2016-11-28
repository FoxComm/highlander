package services.plugins

import scala.concurrent.Future

import dispatch.{Http, as, host, url ⇒ request}
import failures.NotFoundFailure404
import models.plugins._
import models.plugins.PluginSettings.{SettingsSchema, SettingsValues}
import models.plugins.PluginSettings.SettingsValues._
import org.json4s.jackson.JsonMethods._
import payloads.PluginPayloads._
import responses.plugins.PluginCommonResponses._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.JsonFormatters

import com.typesafe.scalalogging.LazyLogging

object PluginsManager extends LazyLogging {

  private def fetchSchemaSettings(
      plugin: Plugin,
      payload: RegisterPluginPayload,
      foundOrCreated: FoundOrCreated)(implicit ec: EC): DbResultT[SettingsSchema] =
    payload.schemaSettings.fold {
      implicit val formats = JsonFormatters.phoenixFormats
      val req              = host(plugin.apiHost, plugin.apiPort) / "_settings" / "schema"
      DbResultT.fromDbio(DBIO.from(Http(req OK as.json4s.Json).map(_.extract[SettingsSchema])))
    }(DbResultT.good(_))

  def uploadNewSettingsToPlugin(plugin: Plugin)(implicit ec: EC): Future[String] = {
    val rawReq = host(plugin.apiHost, plugin.apiPort) / "_settings" / "upload"
    val body   = compact(render(plugin.settings.toJson))
    val req    = rawReq.setContentType("application/json", "UTF-8") << body
    logger.info(s"Updating plugin ${plugin.name} at ${plugin.apiHost}:${plugin.apiPort}: ${body}")
    val res = Http(req.POST OK as.String)
    for {
      r ← res
    } yield {
      logger.info(s"Plugin Response: $r")
      r
    }
  }

  private def updatePluginInfo(
      plugin: Plugin,
      schema: SettingsSchema,
      payload: RegisterPluginPayload)(implicit ec: EC): DbResultT[Plugin] = {
    val newSettings = schema.filterNot { s ⇒
      plugin.settings.contains(s.name)
    }.foldLeft(plugin.settings) { (settings, schemaSetting) ⇒
      settings + (schemaSetting.name → schemaSetting.default)
    }
    Plugins.update(plugin,
                   plugin.copy(settings = newSettings,
                               schemaSettings = schema,
                               apiHost = payload.apiHost,
                               apiPort = payload.apiPort,
                               version = payload.version,
                               description = payload.description))
  }

  private def updatePlugin(plugin: Plugin,
                           payload: RegisterPluginPayload,
                           foundOrCreated: FoundOrCreated)(implicit ec: EC): DbResultT[Plugin] =
    for {
      schema ← * <~ fetchSchemaSettings(plugin, payload, foundOrCreated)
      plugin ← * <~ updatePluginInfo(plugin, schema, payload)
    } yield plugin

  def listPlugins()(implicit ec: EC, db: DB, ac: AC): DbResultT[ListPluginsAnswer] = {
    for {
      plugins ← * <~ Plugins.result
    } yield plugins.map(PluginInfo.fromPlugin)
  }

  def registerPlugin(payload: RegisterPluginPayload)(implicit ec: EC,
                                                     db: DB,
                                                     ac: AC): DbResultT[RegisterAnswer] = {
    val pluginT = for {
      result ← * <~ Plugins
                .findByName(payload.name)
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

  def updateSettings(name: String, payload: UpdateSettingsPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[SettingsUpdated] = {
    val updated = for {
      plugin ← * <~ Plugins.findByName(name).mustFindOr(NotFoundFailure404(Plugin, name))
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

  def listSettings(name: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[SettingsValues] = {
    for {
      plugin ← * <~ Plugins.findByName(name).mustFindOr(NotFoundFailure404(Plugin, name))
    } yield plugin.settings
  }

  def getSettingsWithSchema(
      name: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[PluginSettingsResponse] = {
    for {
      plugin ← * <~ Plugins.findByName(name).mustFindOr(NotFoundFailure404(Plugin, name))
    } yield PluginSettingsResponse.fromPlugin(plugin)
  }

}
