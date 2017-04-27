package responses

import io.circe.syntax._
import models.payment.giftcard.{GiftCard, GiftCardSubtype}
import utils.aliases._
import utils.json.codecs._

object GiftCardSubTypesResponse {
  case class Root(originType: GiftCard.OriginType, subTypes: Seq[GiftCardSubtype])
      extends ResponseItem {
    def json: Json = this.asJson
  }

  def build(originTypes: Seq[GiftCard.OriginType], subTypes: Seq[GiftCardSubtype]): Seq[Root] = {
    originTypes.map { originType ⇒
      Root(originType, subTypes.filter(_.originType == originType))
    }
  }
}
