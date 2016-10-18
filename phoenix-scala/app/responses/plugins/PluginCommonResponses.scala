package responses.plugins

import models.plugins.Plugin
import java.time.Instant

import models.plugins.Plugin._
import models.plugins.PluginSettings._
import responses.plugins.PluginCommonResponses.FullPluginInfo
import utils._

object PluginCommonResponses {

  case class RegisterAnswer(foundOrCreated: String, settings: SettingsValues)

  def buildRegister(plugin: Plugin, foundOrCreated: db.FoundOrCreated): RegisterAnswer = {
    RegisterAnswer(foundOrCreated = foundOrCreated.toString, settings = plugin.settings)
  }

  case class SettingsUpdated(settings: SettingsValues)

  case class PluginInfo(name: String,
                        description: String,
                        version: String,
                        createdAt: Instant,
                        state: State)

  case class FullPluginInfo(name: String,
                            description: String,
                            version: String,
                            createdAt: Instant,
                            state: State,
                            settings: SettingsValues,
                            schemaSettings: SettingsSchema)

  object PluginInfo {
    def fromPlugin(plugin: Plugin): PluginInfo = {
      PluginInfo(name = plugin.name,
                 description = plugin.description,
                 version = plugin.version,
                 createdAt = plugin.createdAt,
                 state = if (plugin.isDisabled) Inactive else Active)
    }
  }

  object FullPluginInfo {
    def fromPlugin(plugin: Plugin): FullPluginInfo = {
      FullPluginInfo(name = plugin.name,
                     description = plugin.description,
                     version = plugin.version,
                     createdAt = plugin.createdAt,
                     state = if (plugin.isDisabled) Inactive else Active,
                     settings = plugin.settings,
                     schemaSettings = plugin.schemaSettings)
    }
  }

  type ListPluginsAnswer = Seq[PluginInfo]

}
