package seeds

import utils.JsonFormatters

package object requests {

  implicit val formats = JsonFormatters.phoenixFormats

}
