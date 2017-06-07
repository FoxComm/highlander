package phoenix.utils.seeds

import phoenix.models.Reason
import phoenix.models.Reason.{Cancellation, GiftCardCreation, StoreCreditCreation}
import phoenix.models.cord.{OrderPayment, OrderShippingAddress}
import phoenix.models.payment.creditcard.CreditCardCharge
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object Factories
    extends CustomerSeeds
    with GiftCardSeeds
    with StoreCreditSeeds
    with ReturnSeeds
    with ProductSeeds
    with ShipmentSeeds
    with OrderSeeds
    with StoreAdminSeeds
    with AddressSeeds
    with CreditCardSeeds
    with CustomersGroupSeeds
    with GroupTemplatesSeeds
    with DiscountSeeds
    with PromotionSeeds
    with ObjectSchemaSeeds
    with CouponSeeds
    with SharedSearchSeeds {

  override implicit val formats = JsonFormatters.phoenixFormats

  def orderPayment = OrderPayment.build(creditCard)

  def giftCardPayment = OrderPayment.build(giftCard)

  def storeCreditPayment(implicit au: AU) = OrderPayment.build(storeCredit)

  def shippingAddress =
    OrderShippingAddress(regionId = 4174,
                         name = "Old Yax",
                         address1 = "9313 Olde Mill Pond Dr",
                         address2 = None,
                         city = "Glen Allen",
                         zip = "23060",
                         phoneNumber = None)

  def creditCardCharge =
    CreditCardCharge(creditCardId = creditCard.id,
                     orderPaymentId = orderPayment.id,
                     stripeChargeId = "foo",
                     amount = 25)

  def reason(storeAdminId: Int) =
    Reason(storeAdminId = storeAdminId, body = "I'm a reason", parentId = None)

  def reasons: Seq[Reason] =
    Seq(
      // Gift card creation reasons
      Reason(body = "Gift to loyal customer",
             reasonType = GiftCardCreation,
             parentId = None,
             storeAdminId = 0),
      Reason(body = "New year GC giveaway", reasonType = GiftCardCreation, parentId = None, storeAdminId = 0),
      // Store credit creation reasons
      Reason(body = "Gift to loyal customer",
             reasonType = StoreCreditCreation,
             parentId = None,
             storeAdminId = 0),
      Reason(body = "New year SC giveaway",
             reasonType = StoreCreditCreation,
             parentId = None,
             storeAdminId = 0),
      // Cancellation reasons
      Reason(body = "Cancelled by customer request",
             reasonType = Cancellation,
             parentId = None,
             storeAdminId = 0),
      Reason(body = "Cancelled because duplication",
             reasonType = Cancellation,
             parentId = None,
             storeAdminId = 0),
      Reason(body = "Other cancellation reason", reasonType = Cancellation, parentId = None, storeAdminId = 0)
    )

  def createSingleMerchantSystem(implicit ec: EC) =
    sql""" select bootstrap_single_merchant_system() """.as[Int]

  def createSecondStageMerchant(implicit ec: EC) =
    sql""" select bootstrap_demo_organization('merchant2', 'merchant2.com', 1, 1) """.as[Int]
}
