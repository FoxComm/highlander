package phoenix.responses

import phoenix.models.payment.storecredit.{StoreCredit, StoreCreditSubtype}

object StoreCreditSubTypesResponse {
  case class Root(originType: StoreCredit.OriginType, subTypes: Seq[StoreCreditSubtype]) extends ResponseItem

  def build(originTypes: Seq[StoreCredit.OriginType], subTypes: Seq[StoreCreditSubtype]): Seq[Root] =
    originTypes.map { originType ⇒
      Root(originType, subTypes.filter(_.originType == originType))
    }
}
