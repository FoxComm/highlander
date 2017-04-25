package services.plugins

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import dispatch.{Http, as, host, url ⇒ request}
import failures.{GeneralFailure, NotFoundFailure404}
import io.circe.Json
import io.circe.jackson.syntax._
import models.plugins.PluginSettings.SettingsValues._
import models.plugins.PluginSettings.{SettingsSchema, SettingsValues}
import models.plugins._
import payloads.PluginPayloads._
import responses.plugins.PluginCommonResponses._
import scala.concurrent.Future
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.json._

object PluginsManager extends LazyLogging {

  private def getSchemaSettings(
      plugin: Plugin,
      payload: RegisterPluginPayload,
      foundOrCreated: FoundOrCreated)(implicit ec: EC): DbResultT[SettingsSchema] =
    payload.schemaSettings.fold {
      plugin
        .apiUrl()
        .fold(DbResultT.failure[SettingsSchema](
                GeneralFailure("settingsSchema or apiUrl should be " +
                      "present"))) { apiUrl ⇒
          val req = host(apiUrl) / "_settings" / "schema"
          DbResultT.fromFEither(DBIO.from(Http(req OK asJson).map(_.as[SettingsSchema].leftMap(ex ⇒
                            GeneralFailure(ex.getMessage()).single))))
        }
    }(DbResultT.good(_))

  def uploadNewSettingsToPlugin(plugin: Plugin)(implicit ec: EC): Future[String] = {
    plugin.apiUrl().fold(Future.successful("")) { apiUrl ⇒
      val rawReq = host(apiUrl) / "_settings" / "upload"
      val body   = Json.fromJsonObject(plugin.settings).jacksonPrint
      val req    = rawReq.setContentType("application/json", "UTF-8") << body
      logger.info(
          s"Updating plugin ${plugin.name} at ${plugin.apiHost}:${plugin.apiPort}: ${body}")
      val resp = Http(req.POST OK as.String)
      resp.map { respBody ⇒
        logger.info(s"Plugin Response: $respBody")
        respBody
      }
    }
  }

  private def updatePluginInfo(
      plugin: Plugin,
      schema: SettingsSchema,
      payload: RegisterPluginPayload)(implicit ec: EC): DbResultT[Plugin] = {
    val newSettings = schema.filterNot { s ⇒
      plugin.settings.contains(s.name)
    }.foldLeft(plugin.settings) { (settings, schemaSetting) ⇒
      settings.add(schemaSetting.name, schemaSetting.default)
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
      schema ← * <~ getSchemaSettings(plugin, payload, foundOrCreated)
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
