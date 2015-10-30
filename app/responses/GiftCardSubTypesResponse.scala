package responses

import models.{GiftCard, GiftCardSubtype}

object GiftCardSubTypesResponse {
  final case class Root(originType: GiftCard.OriginType, subTypes: Seq[GiftCardSubtype]) extends ResponseItem

  def build(originTypes: Seq[GiftCard.OriginType], subTypes: Seq[GiftCardSubtype]): Seq[Root] = {
    originTypes.map { originType ⇒
      Root(originType, subTypes.filter(_.originType == originType))
    }
  }
}
