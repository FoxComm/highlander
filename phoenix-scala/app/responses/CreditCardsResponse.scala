package responses

import models.location.{Region, Regions}
import models.payment.creditcard.CreditCard
import utils.aliases._
import utils.db._

object CreditCardsResponse {
  case class Root(id: Int,
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

  def buildFromCreditCard(cc: CreditCard)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      region ← * <~ Regions.mustFindById400(cc.address.regionId)
    } yield build(cc, region)

  def build(cc: CreditCard, region: Region): Root =
    Root(id = cc.id,
         customerId = cc.accountId,
         holderName = cc.holderName,
         lastFour = cc.lastFour,
         expMonth = cc.expMonth,
         expYear = cc.expYear,
         isDefault = cc.isDefault,
         address1Check = cc.address1Check,
         zipCheck = cc.zipCheck,
         inWallet = cc.inWallet,
         brand = cc.brand,
         address = AddressResponse.buildFromCreditCard(cc, region))

  // Temporary simplified version w/o address
  case class RootSimple(id: Int,
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

  def buildSimple(cc: CreditCard): RootSimple =
    RootSimple(id = cc.id,
               customerId = cc.accountId,
               holderName = cc.holderName,
               lastFour = cc.lastFour,
               expMonth = cc.expMonth,
               expYear = cc.expYear,
               isDefault = cc.isDefault,
               address1Check = cc.address1Check,
               zipCheck = cc.zipCheck,
               inWallet = cc.inWallet,
               brand = cc.brand)
}
