import models.cord.OrderPayments.scope._
import models.cord._
import models.payment.PaymentMethod
import slick.driver.PostgresDriver.api._
import util._
import util.fixtures.BakedFixtures

trait CartPaymentsIntegrationTestBase
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  def paymentsFor(cart: Cart, pmt: PaymentMethod.Type): Seq[OrderPayment] = {
    OrderPayments.filter(_.cordRef === cart.refNum).byType(pmt).gimme
  }

  def creditCardPayments(cart: Cart) =
    paymentsFor(cart, PaymentMethod.CreditCard)
  def giftCardPayments(cart: Cart) =
    paymentsFor(cart, PaymentMethod.GiftCard)
  def storeCreditPayments(cart: Cart) =
    paymentsFor(cart, PaymentMethod.StoreCredit)

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed
}
