package responses

import io.circe.syntax._
import models.payment.storecredit.{StoreCredit, StoreCreditSubtype}
import utils.aliases._
import utils.json.codecs._

object StoreCreditSubTypesResponse {
  case class Root(originType: StoreCredit.OriginType, subTypes: Seq[StoreCreditSubtype])
      extends ResponseItem {
    def json: Json = this.asJson
  }

  def build(originTypes: Seq[StoreCredit.OriginType],
            subTypes: Seq[StoreCreditSubtype]): Seq[Root] = {
    originTypes.map { originType ⇒
      Root(originType, subTypes.filter(_.originType == originType))
    }
  }
}
