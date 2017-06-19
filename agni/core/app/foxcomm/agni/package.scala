package foxcomm

import io.circe.generic.extras.Configuration

package object agni {
  implicit val configuration: Configuration =
    Configuration.default.withDiscriminator("type").withSnakeCaseKeys
}
