package testutils.fixtures

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures.UserFailures.OrganizationNotFoundByName
import models._
import models.account._
import models.auth.UserToken
import models.cord._
import models.inventory.ProductVariant
import models.location._
import models.payment.giftcard.GiftCard
import models.product._
import payloads.OrderPayloads
import payloads.OrderPayloads.CreateCart
import payloads.PaymentPayloads.GiftCardPayment
import services.Authenticator.AuthData
import services.account.AccountManager
import services.carts._
import testutils._
import testutils.fixtures.raw._
import utils.Money.Currency
import utils.aliases._
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
    def storeAdmin: User

    def cart: Cart = _cart

    private val _cart = (for {
      response ← * <~ CartCreator.createCart(storeAdmin, CreateCart(customer.accountId.some))
      cart     ← * <~ Carts.mustFindByRefNum(response.referenceNumber)
    } yield cart).gimme
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

    val simpleSku: ProductVariant =
      Mvp.insertSku(Scope.current, ctx.id, SimpleSku("BY-ITSELF", "A lonely item", 9999)).gimme
  }

  trait ProductWithVariants_Raw extends StoreAdmin_Seed {
    def simpleProduct: Product

    val productWithVariants: (Product, SimpleCompleteVariantData, Seq[ProductVariant]) = {
      val scope = LTree(au.token.scope)

      val testSkus = Seq(SimpleSku("SKU-TST", "SKU test", 1000, Currency.USD, active = true),
                         SimpleSku("SKU-TS2", "SKU test 2", 1000, Currency.USD, active = true))

      val simpleSizeVariant = SimpleCompleteVariant(
          variant = SimpleVariant("Size"),
          variantValues = Seq(SimpleVariantValue("Small", "", Seq("SKU-TST")),
                              SimpleVariantValue("Large", "", Seq("SKU-TS2"))))

      for {
        skus    ← * <~ Mvp.insertSkus(scope, ctx.id, testSkus)
        product ← * <~ Products.mustFindById404(simpleProduct.id)
        variant ← * <~ Mvp.insertVariantWithValues(scope, ctx.id, product, simpleSizeVariant)
      } yield (product, variant, skus)
    }.gimme
  }
}
