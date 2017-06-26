package phoenix.responses.giftcards

import phoenix.models.payment.giftcard.{GiftCard, GiftCardSubtype}
import phoenix.responses.ResponseItem

case class GiftCardSubTypesResponse(originType: GiftCard.OriginType, subTypes: Seq[GiftCardSubtype])
    extends ResponseItem

object GiftCardSubTypesResponse {

  def build(originTypes: Seq[GiftCard.OriginType],
            subTypes: Seq[GiftCardSubtype]): Seq[GiftCardSubTypesResponse] =
    originTypes.map { originType ⇒
      GiftCardSubTypesResponse(originType, subTypes.filter(_.originType == originType))
    }
}
