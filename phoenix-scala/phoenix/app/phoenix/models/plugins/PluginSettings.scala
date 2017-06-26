package phoenix.models.plugins

import com.pellucid.sealerate
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import phoenix.utils.{ADT, JsonFormatters}

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
                        default: JValue)

  type SettingsSchema = Seq[SettingDef]
  type SettingsValues = Map[String, JValue]

  object SettingsValues {
    implicit val formats = JsonFormatters.phoenixFormats

    implicit class SettingsValuesEnriched(val values: SettingsValues) extends AnyVal {
      def merge(other: SettingsValues): SettingsValues = {
        val merged = Extraction.decompose(values) merge Extraction.decompose(other)
        merged.extract[SettingsValues]
      }

      def toJson: JValue =
        Extraction.decompose(values)

    }

  }
}
