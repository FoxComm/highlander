package foxcomm.search

import io.circe.generic.extras.Configuration

package object dsl {
  implicit def configuration: Configuration = foxcomm.search.configuration
}
