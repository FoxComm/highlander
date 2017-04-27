package models.plugins

import com.pellucid.sealerate
import io.circe._
import utils.ADT

object PluginSettings {
  sealed trait SettingType
  case object String extends SettingType
  case object Number extends SettingType
  case object Bool   extends SettingType
  case object Text   extends SettingType

  object SettingType extends ADT[SettingType] {
    def types = sealerate.values[SettingType]
  }

  case class SettingDef(name: String,
                        title: String,
                        description: Option[String],
                        `type`: SettingType,
                        default: Json)

  type SettingsSchema = Seq[SettingDef]
  type SettingsValues = JsonObject

  object SettingsValues {
    implicit class SettingsValuesEnriched(val values: SettingsValues) extends AnyVal {
      def merge(other: SettingsValues): SettingsValues = {
        val merged = Json.fromJsonObject(values) deepMerge Json.fromJsonObject(other)
        merged.asObject.getOrElse(values)
      }
    }

  }
}
