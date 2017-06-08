package phoenix.responses

import phoenix.models.payment.giftcard.{GiftCard, GiftCardSubtype}

object GiftCardSubTypesResponse {
  case class Root(originType: GiftCard.OriginType, subTypes: Seq[GiftCardSubtype]) extends ResponseItem

  def build(originTypes: Seq[GiftCard.OriginType], subTypes: Seq[GiftCardSubtype]): Seq[Root] =
    originTypes.map { originType ⇒
      Root(originType, subTypes.filter(_.originType == originType))
    }
}
