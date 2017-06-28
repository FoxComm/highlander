package testutils.fixtures

import java.time.Instant

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import core.db._
import core.utils.Money.Currency
import phoenix.models.Reason.Cancellation
import phoenix.models._
import phoenix.models.account._
import phoenix.models.cord._
import phoenix.models.inventory.Sku
import phoenix.models.location._
import phoenix.models.payment.giftcard.GiftCard
import phoenix.models.product._
import phoenix.payloads.CartPayloads.CreateCart
import phoenix.payloads.PaymentPayloads.GiftCardPayment
import phoenix.services.carts._
import phoenix.utils.seeds.Factories
import testutils._
import testutils.fixtures.raw._

/**
  * Raw fixtures are cake-pattern definitions.
  * Each trait declares "dependencies" that must be satisfied before fixture can be created.
  */
trait RawFixtures extends RawPaymentFixtures with TestSeeds {

  // Simple models
  trait Reason_Raw {
    def storeAdmin: User

    private val r: Reason          = Factories.reason(storeAdmin.accountId)
    val reason: Reason             = Reasons.create(r).gimme
    val cancellationReason: Reason = Reasons.create(r.copy(reasonType = Cancellation)).gimme
  }

  trait CustomerAddress_Raw {
    def customer: User

    def address: Address = _address

    private val _address: Address = Addresses
      .create(Factories.address.copy(accountId = customer.accountId, isDefaultShipping = true))
      .gimme

    lazy val region: Region = Regions.findOneById(address.regionId).unsafeGet.gimme
  }

  // Cart
  trait EmptyCart_Raw extends StoreAdmin_Seed {

    def customer: User
    def storeAdmin: User

    def cart: Cart = _cart

    private val _cart = {
      (for {
        response ← * <~ CartCreator.createCart(storeAdmin, CreateCart(customer.accountId.some))
        cart     ← * <~ Carts.mustFindByRefNum(response.referenceNumber)
      } yield cart).gimme
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
    def gcPaymentAmount: Long

    private val payload = GiftCardPayment(code = giftCard.code, amount = gcPaymentAmount.some)

    CartPaymentUpdater.addGiftCard(storeAdmin, payload, cart.refNum.some).gimme

    override def orderPayments =
      super.orderPayments ++ OrderPayments
        .findAllGiftCardsByCordRef(cart.refNum)
        .map { case (orderPayment, _) ⇒ orderPayment }
        .gimme
  }

  trait CartWithGiftCardOnlyPayment_Raw extends CartWithGiftCardPayment_Raw {
    override def gcPaymentAmount: Long = giftCard.availableBalance
  }

  trait FullCart_Raw extends EmptyCart_Raw with CartWithPayments_Raw

  // Order
  trait Order_Raw extends Customer_Seed {

    implicit lazy val au = customerAuthData

    def cart: Cart

    val order: Order = Orders.createFromCart(cart, None).gimme
  }

  // Product
  trait Product_Raw extends StoreAdmin_Seed {

    val simpleProduct: Product = {
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
    }.gimme
  }

  trait Sku_Raw extends StoreAdmin_Seed {

    val simpleSku: Sku = Mvp
      .insertSku(Scope.current, ctx.id, SimpleSku("BY-ITSELF", "A lonely item", 9999, active = true))
      .gimme
  }

  trait ProductWithVariants_Raw extends StoreAdmin_Seed {
    def simpleProduct: Product

    val productWithVariants: (Product, SimpleCompleteVariantData, Seq[Sku]) = {
      val scope = LTree(au.token.scope)

      val testSkus = Seq(SimpleSku("SKU-TST", "SKU test", 1000, Currency.USD, active = true),
                         SimpleSku("SKU-TS2", "SKU test 2", 1000, Currency.USD, active = true))

      val simpleSizeVariant = SimpleCompleteVariant(variant = SimpleVariant("Size"),
                                                    variantValues =
                                                      Seq(SimpleVariantValue("Small", "", Seq("SKU-TST")),
                                                          SimpleVariantValue("Large", "", Seq("SKU-TS2"))))

      for {
        skus    ← * <~ Mvp.insertSkus(scope, ctx.id, testSkus)
        product ← * <~ Products.mustFindById404(simpleProduct.id)
        variant ← * <~ Mvp.insertVariantWithValues(scope, ctx.id, product, simpleSizeVariant)
      } yield (product, variant, skus)
    }.gimme
  }
}
