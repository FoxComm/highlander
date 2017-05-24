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
                                   apiHost: Option[String],
                                   apiPort: Option[Int],
                                   schemaSettings: Option[SettingsSchema])
      extends Validation[RegisterPluginPayload] {

    def isManaged(): Boolean = {
      apiPort.isDefined && apiHost.isDefined
    }

    def apiUrlNotEmptyIfManaged(): ValidatedNel[Failure, Unit] = {
      (notEmptyIf(schemaSettings,
                  !isManaged(),
                  "schemaSettings or apiHost & apiPort should be presented")
            |@| apiPort.fold(ok) { port ⇒
              greaterThan(port, 0, "Api port")
            }).map { case _ ⇒ () }
    }

    def validate: ValidatedNel[Failure, RegisterPluginPayload] = {
      (notEmpty(name, "name")
            |@| notEmpty(version, "version")
            |@| nullOrNotEmpty(apiHost, "API host")
            |@| apiUrlNotEmptyIfManaged()).map { case _ ⇒ this }
    }
  }

  case class UpdateSettingsPayload(settings: SettingsValues)

}