package gatling.seeds

import phoenix.utils.JsonFormatters

package object requests {

  implicit val formats = JsonFormatters.phoenixFormats
}
