package plugins

trait PluginSettings {
  def isDisabled: Boolean
}

trait Plugin {
  def identifier: String
  def settings: PluginSettings
}

object PluginRegistry {}
