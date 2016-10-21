package testutils.fixtures

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import models._
import models.account._
import models.cord._
import models.inventory.Sku
import models.location._
import models.payment.giftcard.GiftCard
import models.product._
import payloads.OrderPayloads
import payloads.PaymentPayloads.GiftCardPayment
import services.carts._
import testutils._
import testutils.fixtures.raw._
import utils.db._
import utils.seeds.Seeds.Factories

/**
  * Raw fixtures are cake-pattern definitions.
  * Each trait declares "dependencies" that must be satisfied before fixture can be created.
  */
trait RawFixtures extends RawPaymentFixtures with TestSeeds {

  // Simple models
  trait Reason_Raw {
    def storeAdmin: User

    val reason: Reason = Reasons.create(Factories.reason(storeAdmin.accountId)).gimme
  }

  trait CustomerAddress_Raw {
    def customer: User

    def address: Address = _address

    private val _address: Address = Addresses
      .create(Factories.address.copy(accountId = customer.accountId, isDefaultShipping = true))
      .gimme

    lazy val region: Region = Regions.findOneById(address.regionId).safeGet.gimme
  }

  // Cart
  trait EmptyCart_Raw extends StoreAdmin_Seed {
    def customer: User

    def cart: Cart = _cart

    private val _cart: Cart = {
      val payload  = OrderPayloads.CreateCart(customerId = customer.accountId.some)
      val response = CartCreator.createCart(storeAdmin, payload).gimme
      Carts.mustFindByRefNum(response.referenceNumber).gimme
    }
  }

  trait CartWithShipAddress_Raw {
    def address: Address
    def cart: Cart
    def storeAdmin: User

    val shippingAddress: OrderShippingAddress = {
      CartShippingAddressUpdater
        .createShippingAddressFromAddressId(storeAdmin, address.id, cart.refNum.some)
        .gimme
      OrderShippingAddresses.findByOrderRef(cart.refNum).gimme.head
    }
  }

  trait CartWithPayments_Raw {
    def cart: Cart

    def orderPayments: Seq[OrderPayment] = Seq.empty
  }

  trait CartWithGiftCardPayment_Raw extends CartWithPayments_Raw {
    def storeAdmin: User
    def giftCard: GiftCard
    def gcPaymentAmount: Int

    private val payload = GiftCardPayment(code = giftCard.code, amount = gcPaymentAmount.some)

    CartPaymentUpdater.addGiftCard(storeAdmin, payload, cart.refNum.some).gimme

    override def orderPayments =
      super.orderPayments ++ OrderPayments
        .findAllGiftCardsByCordRef(cart.refNum)
        .map { case (orderPayment, _) ⇒ orderPayment }
        .gimme
  }

  trait CartWithGiftCardOnlyPayment_Raw extends CartWithGiftCardPayment_Raw {
    override def gcPaymentAmount: Int = giftCard.availableBalance
  }

  trait FullCart_Raw extends EmptyCart_Raw with CartWithPayments_Raw

  // Order
  trait Order_Raw {
    def cart: Cart

    val order: Order = Orders.createFromCart(cart).gimme
  }

  // Product
  trait Product_Raw extends StoreAdmin_Seed {

    val simpleProduct: Product = ({
      for {
        spd ← * <~ Mvp.insertProduct(ctx.id,
                                     SimpleProductData(title = "Test Product",
                                                       code = "TEST",
                                                       description = "Test product description",
                                                       image = "image.png",
                                                       price = 5999,
                                                       active = true))
        pd ← * <~ Products.mustFindById404(spd.productId)
      } yield pd
    }).gimme
  }

  trait Sku_Raw extends StoreAdmin_Seed {

    val simpleSku: Sku =
      Mvp.insertSku(Scope.current, ctx.id, SimpleSku("BY-ITSELF", "A lonely item", 9999)).gimme
  }
}
