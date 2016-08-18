package responses.plugins

import models.plugins.Plugin
import java.time.Instant
import models.plugins.PluginSettings.SettingsValues
import utils.db
import utils.aliases._

object PluginCommonResponses {

  case class RegisterAnswer(foundOrCreated: String, settings: SettingsValues)

  def buildRegister(plugin: Plugin, foundOrCreated: db.FoundOrCreated): RegisterAnswer = {
    RegisterAnswer(foundOrCreated = foundOrCreated.toString, settings = plugin.settings)
  }

  case class SettingsUpdated(settings: SettingsValues)

  case class PluginInfo(name: String, description: String, version: String, createdAt: Instant)

  object PluginInfo {
    def fromPlugin(plugin: Plugin): PluginInfo = {
      PluginInfo(name = plugin.name,
                 description = plugin.description,
                 version = plugin.version,
                 createdAt = plugin.createdAt)
    }
  }

  type ListPluginsAnswer = Seq[PluginInfo]

}
