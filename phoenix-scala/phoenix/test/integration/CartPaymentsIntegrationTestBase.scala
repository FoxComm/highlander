import phoenix.models.cord.OrderPayments.scope._
import phoenix.models.cord._
import phoenix.models.payment.PaymentMethod
import slick.jdbc.PostgresProfile.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

trait CartPaymentsIntegrationTestBase
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  def paymentsFor(cart: Cart, pmt: PaymentMethod.Type): Seq[OrderPayment] =
    OrderPayments.filter(_.cordRef === cart.refNum).byType(pmt).gimme

  def creditCardPayments(cart: Cart): Seq[OrderPayment] =
    paymentsFor(cart, PaymentMethod.CreditCard)

  def giftCardPayments(cart: Cart): Seq[OrderPayment] =
    paymentsFor(cart, PaymentMethod.GiftCard)

  def storeCreditPayments(cart: Cart): Seq[OrderPayment] =
    paymentsFor(cart, PaymentMethod.StoreCredit)

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed
}
