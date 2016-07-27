package responses.plugins

import models.plugins.Plugin
import models.plugins.PluginSettings.SettingsValues
import utils.db
import utils.aliases._

object PluginCommonResponses {

  case class RegisterAnswer(foundOrCreated: String)

  def buildRegister(plugin: Plugin, foundOrCreated: db.FoundOrCreated): RegisterAnswer = {
    RegisterAnswer(foundOrCreated = foundOrCreated.toString)
  }

  case class SettingsUpdated(settings: SettingsValues)

}
