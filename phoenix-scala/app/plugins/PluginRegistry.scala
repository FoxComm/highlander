package plugins

import com.typesafe.scalalogging.LazyLogging
import org.json4s.JsonAST.JValue

trait PluginSettings {
  def isDisabled: Boolean
}

trait Plugin extends LazyLogging {
  def identifier: String
  def settings: PluginSettings

  def receiveSettings(isDisabled: Boolean, newSettings: Map[String, JValue]): Unit

  def register(): Unit = {
    logger.info(s"Registering plugin $identifier")
    PluginRegistry.registerPlugin(identifier, this)
  }
}

object PluginRegistry extends LazyLogging {

  private var registry: Map[String, Plugin] = Map()

  def registerPlugin(identifier: String, plugin: Plugin) = {
    registry = registry ++ Map(identifier → plugin)
    logger.info(s"Plugin ${plugin.identifier} registered")
  }

  def notifySettingsChange(identifier: String,
                           isDisabled: Boolean,
                           newSettings: Map[String, JValue]) = {
    registry.get(identifier).map { plugin ⇒
      logger.info(s"Plugin ${plugin.identifier} settings were updated")
      plugin.receiveSettings(isDisabled, newSettings)
    }
  }

}
