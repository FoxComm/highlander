package plugins

import org.json4s.JsonAST.JValue

trait PluginSettings {
  def isDisabled: Boolean
}

trait Plugin {
  def identifier: String
  def settings: PluginSettings

  def receiveSettings(isDisabled: Boolean, newSettings: Map[String, JValue]): Unit

  def register(): Unit = {
    println(s"Registering plugin $identifier")
    PluginRegistry.registerPlugin(identifier, this)
  }
}

object PluginRegistry {

  private var registry: Map[String, Plugin] = Map()

  def registerPlugin(identifier: String, plugin: Plugin) = {
    registry = registry ++ Map(identifier → plugin)
    println(s"Plugin ${plugin.identifier} registered")
  }

  def notifySettingsChange(identifier: String,
                           isDisabled: Boolean,
                           newSettings: Map[String, JValue]) = {
    registry.get(identifier).map { plugin ⇒
      println(s"Plugin ${plugin.identifier} settings were updated")
      plugin.receiveSettings(isDisabled, newSettings)
    }
  }

}
