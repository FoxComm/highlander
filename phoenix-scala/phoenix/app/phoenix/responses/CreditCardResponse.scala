package phoenix.responses

import core.db._
import phoenix.models.location.{Region, Regions}
import phoenix.models.payment.creditcard.CreditCard

case class CreditCardResponse(id: Int,
                              customerId: Int,
                              holderName: String,
                              lastFour: String,
                              expMonth: Int,
                              expYear: Int,
                              isDefault: Boolean = false,
                              address1Check: Option[String] = None,
                              zipCheck: Option[String] = None,
                              inWallet: Boolean = true,
                              brand: String,
                              address: AddressResponse)
    extends ResponseItem

object CreditCardResponse {

  def buildFromCreditCard(cc: CreditCard)(implicit ec: EC, db: DB): DbResultT[CreditCardResponse] =
    for {
      region ← * <~ Regions.mustFindById400(cc.address.regionId)
    } yield build(cc, region)

  def build(cc: CreditCard, region: Region): CreditCardResponse =
    CreditCardResponse(
      id = cc.id,
      customerId = cc.accountId,
      holderName = cc.holderName,
      lastFour = cc.lastFour,
      expMonth = cc.expMonth,
      expYear = cc.expYear,
      isDefault = cc.isDefault,
      inWallet = cc.inWallet,
      brand = cc.brand,
      address = AddressResponse.buildFromCreditCard(cc, region)
    )
}

case class CreditCardNoAddressResponse(id: Int,
                                       customerId: Int,
                                       holderName: String,
                                       lastFour: String,
                                       expMonth: Int,
                                       expYear: Int,
                                       isDefault: Boolean = false,
                                       address1Check: Option[String] = None,
                                       zipCheck: Option[String] = None,
                                       inWallet: Boolean = true,
                                       brand: String)
    extends ResponseItem

object CreditCardNoAddressResponse {
  def build(cc: CreditCard): CreditCardNoAddressResponse =
    CreditCardNoAddressResponse(
      id = cc.id,
      customerId = cc.accountId,
      holderName = cc.holderName,
      lastFour = cc.lastFour,
      expMonth = cc.expMonth,
      expYear = cc.expYear,
      isDefault = cc.isDefault,
      inWallet = cc.inWallet,
      brand = cc.brand
    )
}
