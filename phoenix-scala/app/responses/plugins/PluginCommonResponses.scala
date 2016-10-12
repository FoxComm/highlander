package responses.plugins

import models.plugins.Plugin
import java.time.Instant

import com.pellucid.sealerate
import models.plugins.PluginSettings.SettingsValues
import utils._

object PluginCommonResponses {

  case class RegisterAnswer(foundOrCreated: String, settings: SettingsValues)

  def buildRegister(plugin: Plugin, foundOrCreated: db.FoundOrCreated): RegisterAnswer = {
    RegisterAnswer(foundOrCreated = foundOrCreated.toString, settings = plugin.settings)
  }

  sealed trait State
  case object Active   extends State
  case object Inactive extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  case class SettingsUpdated(settings: SettingsValues)

  case class PluginInfo(name: String,
                        description: String,
                        version: String,
                        createdAt: Instant,
                        state: State)

  object PluginInfo {
    def fromPlugin(plugin: Plugin): PluginInfo = {
      PluginInfo(name = plugin.name,
                 description = plugin.description,
                 version = plugin.version,
                 createdAt = plugin.createdAt,
                 state = if (plugin.isDisabled) Inactive else Active)
    }
  }

  type ListPluginsAnswer = Seq[PluginInfo]

}
