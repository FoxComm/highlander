package models.discount

import models.objects._
import utils.aliases._

object DiscountHelpers {

  implicit class DiscountAttributeExtractor(fs: FormAndShadow) {
    def offer: Json     = fs.getAttribute("offer")
    def qualifier: Json = fs.getAttribute("qualifier")
  }
}
