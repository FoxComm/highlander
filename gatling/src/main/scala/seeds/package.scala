import utils.JsonFormatters

package object seeds {

  implicit val formats = JsonFormatters.phoenixFormats

}
