package phoenix.responses

import phoenix.models.payment.storecredit.{StoreCredit, StoreCreditSubtype}

case class StoreCreditSubTypesResponse(originType: StoreCredit.OriginType, subTypes: Seq[StoreCreditSubtype])
    extends ResponseItem

object StoreCreditSubTypesResponse {

  def build(originTypes: Seq[StoreCredit.OriginType],
            subTypes: Seq[StoreCreditSubtype]): Seq[StoreCreditSubTypesResponse] =
    originTypes.map { originType ⇒
      StoreCreditSubTypesResponse(originType, subTypes.filter(_.originType == originType))
    }
}
