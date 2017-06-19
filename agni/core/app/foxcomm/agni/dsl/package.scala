package foxcomm.agni

import io.circe.generic.extras.Configuration

package object dsl {
  implicit def configuration: Configuration = foxcomm.agni.configuration
}
