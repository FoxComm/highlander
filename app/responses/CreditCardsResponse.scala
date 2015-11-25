package responses

import java.time.Instant
import scala.concurrent.ExecutionContext

import models.{CreditCard, OrderShippingAddresses, Address, Customer, OrderShippingAddress, Region}
import services.NotFoundFailure404
import utils.Slick.DbResult
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object CreditCardsResponse {
  final case class Root(id: Int, customerId: Int, holderName: String, lastFour: String, expMonth: Int, expYear: Int,
    isDefault: Boolean = false, address1Check: Option[String] = None, zipCheck: Option[String] = None,
    inWallet: Boolean = true, brand: String, address: responses.Addresses.Root) extends ResponseItem

  def build(cc: CreditCard, region: Region): Root =
    Root(id = cc.id, customerId = cc.customerId, holderName = cc.holderName, lastFour = cc.lastFour,
    expMonth = cc.expMonth, expYear = cc.expYear, isDefault = cc.isDefault, address1Check = cc.address1Check,
    zipCheck = cc.zipCheck, inWallet = cc.inWallet, brand = cc.brand,
    address = responses.Addresses.buildFromCreditCard(cc, region))
}

