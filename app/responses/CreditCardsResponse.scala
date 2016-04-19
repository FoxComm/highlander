package responses

import models.location.Region
import models.payment.creditcard.CreditCard

object CreditCardsResponse {
  case class Root(id: Int, customerId: Int, holderName: String, lastFour: String, expMonth: Int, expYear: Int,
    isDefault: Boolean = false, address1Check: Option[String] = None, zipCheck: Option[String] = None,
    inWallet: Boolean = true, brand: String, address: responses.Addresses.Root) extends ResponseItem

  def build(cc: CreditCard, region: Region): Root =
    Root(id = cc.id, customerId = cc.customerId, holderName = cc.holderName, lastFour = cc.lastFour,
    expMonth = cc.expMonth, expYear = cc.expYear, isDefault = cc.isDefault, address1Check = cc.address1Check,
    zipCheck = cc.zipCheck, inWallet = cc.inWallet, brand = cc.brand,
    address = responses.Addresses.buildFromCreditCard(cc, region))

  // Temporary simplified version w/o address
  case class RootSimple(id: Int, customerId: Int, holderName: String, lastFour: String, expMonth: Int,
    expYear: Int, isDefault: Boolean = false, address1Check: Option[String] = None, zipCheck: Option[String] = None,
    inWallet: Boolean = true, brand: String) extends ResponseItem

  def buildSimple(cc: CreditCard): RootSimple =
    RootSimple(id = cc.id, customerId = cc.customerId, holderName = cc.holderName, lastFour = cc.lastFour,
      expMonth = cc.expMonth, expYear = cc.expYear, isDefault = cc.isDefault, address1Check = cc.address1Check,
      zipCheck = cc.zipCheck, inWallet = cc.inWallet, brand = cc.brand)
}

