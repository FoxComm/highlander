package payloads

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import utils.Validation
import utils.Validation._
import utils.aliases._
import models.plugins.PluginSettings._

object PluginPayloads {

  case class RegisterPluginPayload(name: String,
                                   version: String,
                                   description: String,
                                   apiHost: String,
                                   apiPort: Int,
                                   schemaSettings: Option[SettingsSchema])
      extends Validation[RegisterPluginPayload] {

    def validate: ValidatedNel[Failure, RegisterPluginPayload] = {
      (notEmpty(name, "name")
        |@| notEmpty(version, "version")
        |@| notEmpty(apiHost, "API host")
        |@| greaterThan(apiPort, 1, "Api port")).map { case _ ⇒ this }
    }
  }

  case class UpdateSettingsPayload(settings: SettingsValues)

}
