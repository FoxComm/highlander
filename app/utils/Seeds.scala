package utils

import java.time.{ZoneId, Instant}
import java.time.temporal.ChronoField

import scala.concurrent.Await
import scala.concurrent.duration._

import models._
import models.rules._
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import slick.dbio
import slick.dbio.Effect.{All, Write}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import utils.Money.Currency

object Seeds {

  def main(args: Array[String]): Unit = {
    Console.err.println(s"Cleaning DB and running migrations")
    val config: com.typesafe.config.Config = utils.Config.loadWithEnv()
    flyWayMigrate(config)
    Console.err.println(s"Inserting seeds")
    implicit val db: PostgresDriver.backend.DatabaseDef = Database.forConfig("db", config)
    Await.result(db.run(run()), 5.second)
  }

  val today = Instant.now().atZone(ZoneId.of("UTC"))

  final case class TheWorld(customers: Seq[Customer], order: Order, orderNotes: Seq[Note], address: Address,
    cc: CreditCard, storeAdmin: StoreAdmin, shippingAddresses: Seq[OrderShippingAddress],
    shippingMethods: Seq[ShippingMethod], shippingPriceRules: Seq[ShippingPriceRule],
    shippingMethodRuleMappings: Seq[ShippingMethodPriceRule], skus: Seq[Sku], orderLineItems: Seq[OrderLineItem],
    orderPayments: Seq[OrderPayment], shipment: Shipment, paymentMethods: AllPaymentMethods, reasons: Seq[Reason],
    orderLineItemSkus: Seq[OrderLineItemSku], inventorySummaries: Seq[InventorySummary])

  final case class AllPaymentMethods(giftCard: GiftCard = Factories.giftCard, storeCredit: StoreCredit = Factories
    .storeCredit)

  def run()(implicit db: Database): dbio.DBIOAction[(Option[Int], Order, Address, OrderShippingAddress, CreditCard,
    GiftCard, StoreCredit), NoStream, All] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val s = TheWorld(
      customers = Factories.customers,
      storeAdmin = Factories.storeAdmin,
      skus = Factories.skus,
      order = Factories.order,
      orderNotes = Factories.orderNotes,
      address = Factories.address,
      shippingAddresses = Seq(Factories.shippingAddress),
      shippingMethods = Factories.shippingMethods,
      cc = Factories.creditCard,
      shippingPriceRules = Factories.shippingPriceRules,
      shippingMethodRuleMappings = Factories.shippingMethodRuleMappings,
      orderLineItems = Factories.orderLineItems,
      orderPayments = Seq(Factories.orderPayment),
      shipment = Factories.shipment,
      paymentMethods = AllPaymentMethods(giftCard = Factories.giftCard, storeCredit = Factories.storeCredit),
      reasons = Factories.reasons,
      orderLineItemSkus = Factories.orderLineItemSkus,
      inventorySummaries = Factories.inventorySummaries
    )

    s.address.validate.fold(err ⇒ throw new Exception(err.mkString("\n")), _ ⇒ {})
    s.storeAdmin.validate.fold(err ⇒ throw new Exception(err.mkString("\n")), _ ⇒ {})
    s.order.validate.fold(err ⇒ throw new Exception(err.mkString("\n")), _ ⇒ {})
    s.cc.validate.fold(err ⇒ throw new Exception(err.mkString("\n")), _ ⇒ {})

    val failures = s.customers.map { _.validate }.filterNot(_.isValid)

    if (failures.nonEmpty)
      throw new Exception(failures.map(_.mkString("\n")).mkString("\n"))

    for {
      customer ← (Customers.returningId += Factories.customer).map(id => Factories.customer.copy(id = id))
      customers ← Customers ++= s.customers
      storeAdmin ← (StoreAdmins.returningId += s.storeAdmin).map(id => s.storeAdmin.copy(id = id))
      skus ← Skus ++= s.skus
      summaries ← InventorySummaries ++= s.inventorySummaries
      order ← Orders.create(s.order.copy(customerId = customer.id))
      orderNotes ← Notes ++= s.orderNotes
      orderLineItemOrigins ← OrderLineItemSkus ++= s.orderLineItemSkus
      orderLineItem ← OrderLineItems ++= s.orderLineItems
      address ← Addresses.save(s.address.copy(customerId = customer.id))
      shippingAddress ← OrderShippingAddresses.save(Factories.shippingAddress.copy(orderId = order.id))
      shippingMethods ← ShippingMethods ++= s.shippingMethods
      creditCard ← CreditCards.save(s.cc.copy(customerId = customer.id))
      orderPayments ← OrderPayments.save(Factories.orderPayment.copy(orderId = order.id,
        paymentMethodId = creditCard.id))
      shippingPriceRule ← ShippingPriceRules ++= s.shippingPriceRules
      shippingMethodRuleMappings ← ShippingMethodsPriceRules ++= s.shippingMethodRuleMappings
      shipments ← Shipments.save(s.shipment)
      reasons ← Reasons ++= s.reasons.map(_.copy(storeAdminId = storeAdmin.id))
      gcOrigin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = storeAdmin.id, reasonId = 1))
      giftCard ← GiftCards.save(s.paymentMethods.giftCard.copy(originId = gcOrigin.id))
      gcAdjustments ← GiftCards.auth(giftCard, Some(orderPayments.id), 10)
      scOrigin ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = storeAdmin.id, reasonId = 1))
      storeCredit ← StoreCredits.save(s.paymentMethods.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
      storeCreditAdjustments ← StoreCredits.auth(storeCredit, Some(orderPayments.id), 10)
    } yield (customers, order, address, shippingAddress, creditCard, giftCard, storeCredit)
  }

  object Factories {
    def customer = Customer(email = "yax@yax.com", password = "password",
      firstName = "Yax", lastName = "Fuentes", phoneNumber = Some("123-444-4388"),
      location = Some("DonkeyVille, TN"), modality = Some("Desktop[PC]"))

    def customers: Seq[Customer] = Seq(
      Customer(email = "adil@adil.com", password = "password",
        firstName = "Adil", lastName = "Wali", phoneNumber = Some("123-444-0909"),
        location = Some("DonkeyHill, WA"), modality = Some("Desktop[PC]")),

      Customer(email = "tivs@tivs.com", password = "password",
        firstName = "Jonathan", lastName = "Rainey", phoneNumber = Some("858-867-5309"),
        location = Some("DonkeyTown, NY"), modality = Some("Desktop[PC]")),

      Customer(email = "cam@cam.com", password = "password",
        firstName = "Cameron", lastName = "Stitt", phoneNumber = Some("883-444-4321"),
        location = Some("Donkeysburg, AU"), modality = Some("Desktop[PC]"))
    )

    def storeAdmin = StoreAdmin(email = "admin@admin.com", password = "password", firstName = "Frankly", lastName = "Admin")

    def order = Order(customerId = 0, referenceNumber = "ABCD1234-11", status = Order.ManualHold)

    def cart = order.copy(status = Order.Cart)

    def orderNotes: Seq[Note] = Seq(
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = "This customer is a donkey."),
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = "No, seriously."),
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = "Like, an actual donkey."),
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = "How did a donkey even place an order on our website?")
    )

    def orderPayment = OrderPayment.build(creditCard)

    def giftCardPayment = OrderPayment.build(giftCard)

    def storeCreditPayment = OrderPayment.build(storeCredit)

    def skus: Seq[Sku] = Seq(
      Sku(sku = "SKU-YAX", name = Some("Flonkey"), price = 33),
      Sku(sku = "SKU-ABC", name = Some("Shark"), price = 45),
      Sku(sku = "SKU-ZYA", name = Some("Dolphin"), price = 88))

    def inventorySummaries: Seq[InventorySummary] = Seq(
      InventorySummary.buildNew(skuId = 1, availableOnHand = 100),
      InventorySummary.buildNew(skuId = 2, availableOnHand = 100),
      InventorySummary.buildNew(skuId = 3, availableOnHand = 100))

    def orderLineItemSkus: Seq[OrderLineItemSku] = Seq(
      OrderLineItemSku(id = 0, orderId = 1, skuId = 1),
      OrderLineItemSku(id = 0, orderId = 1, skuId = 2),
      OrderLineItemSku(id = 0, orderId = 1, skuId = 3))

    def orderLineItems: Seq[OrderLineItem] = Seq(
      OrderLineItem(id = 0, orderId = 1, originId = 1, originType = OrderLineItem.SkuItem, status = OrderLineItem.Cart),
      OrderLineItem(id = 0, orderId = 1, originId = 2, originType = OrderLineItem.SkuItem, status = OrderLineItem.Cart),
      OrderLineItem(id = 0, orderId = 1, originId = 3, originType = OrderLineItem.SkuItem, status = OrderLineItem.Cart))

    def address = Address(customerId = 0, regionId = 4177, name = "Home", address1 = "555 E Lake Union St.",
        address2 = None, city = "Seattle", zip = "12345", isDefaultShipping = true, phoneNumber = None)

    def shippingAddress = OrderShippingAddress(regionId = 4174, name = "Old Yax", address1 = "9313 Olde Mill Pond Dr",
      address2 = None, city = "Glen Allen", zip = "23060", phoneNumber = None)

    def creditCard = {
      CreditCard(customerId = 0, gatewayCustomerId = "cus_6uzC8j5doSTWth", gatewayCardId = "", holderName = "Yax", lastFour = "4242",
        expMonth = today.getMonthValue, expYear = today.getYear + 2, isDefault = true,
        regionId = 4129, addressName = "Old Jeff", address1 = "95 W. 5th Ave.", address2 = Some("Apt. 437"),
        city = "San Mateo", zip = "94402")
    }

    def reason = Reason(id = 0, storeAdminId = 0, body = "I'm a reason", parentId = None)

    def reasons: Seq[Reason] = Seq(
      Reason(body = "Other", parentId = None, storeAdminId = 0),
      Reason(body = "Cancelled by customer request", parentId = None, storeAdminId = 0),
      Reason(body = "Cancelled because duplication", parentId = None, storeAdminId = 0))

    def storeCredit = StoreCredit(customerId = 0, originId = 0, originType = StoreCredit.CsrAppeasement, originalBalance = 50,
      currency = Currency.USD)

    def storeCreditManual = StoreCreditManual(adminId = 0, reasonId = 0)

    def giftCard = GiftCard(currency = Currency.USD, originId = 0, originType = GiftCard.CsrAppeasement,
      originalBalance = 50)

    def giftCardManual = GiftCardManual(adminId = 0, reasonId = 0)

    def giftCardAdjustment = GiftCardAdjustment.build(giftCard, giftCardPayment)

    def shippingMethods = Seq(
      ShippingMethod(adminDisplayName = "UPS Ground", storefrontDisplayName = "UPS Ground", price = 10,
        isActive = true),
      ShippingMethod(adminDisplayName = "UPS Next day", storefrontDisplayName = "UPS Next day", price = 20,
        isActive = true),
      ShippingMethod(adminDisplayName = "DHL Express", storefrontDisplayName = "DHL Express", price = 25,
        isActive = true)
    )

    def shippingPriceRules = Seq(
      ShippingPriceRule(name = "Flat Shipping Over 20", ruleType = ShippingPriceRule.Flat, flatPrice = 10000, flatMarkup = 0),
      ShippingPriceRule(name = "Flat Shipping Over 50", ruleType = ShippingPriceRule.Flat, flatPrice =  5000, flatMarkup = 0),
      ShippingPriceRule(name = "Flat Shipping Over 100", ruleType = ShippingPriceRule.Flat, flatPrice = 1000, flatMarkup = 0)
    )

    def shippingMethodRuleMappings = Seq(
      ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 1, ruleRank = 1),
      ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 2, ruleRank = 2),
      ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 3, ruleRank = 3),

      ShippingMethodPriceRule(shippingMethodId = 2, shippingPriceRuleId = 1, ruleRank = 1),
      ShippingMethodPriceRule(shippingMethodId = 2, shippingPriceRuleId = 2, ruleRank = 2),
      ShippingMethodPriceRule(shippingMethodId = 2, shippingPriceRuleId = 3, ruleRank = 3),

      ShippingMethodPriceRule(shippingMethodId = 3, shippingPriceRuleId = 1, ruleRank = 1)
    )

    def shipment = Shipment(1, 1, Some(1), Some(1))

    def condition = Condition(rootObject = "Order", field = "subtotal", operator = Condition.Equals, valInt = Some(50))
  }

  private def flyWayMigrate(config: com.typesafe.config.Config): Unit = {
    val flyway = new Flyway
    flyway.setDataSource(jdbcDataSourceFromConfig("db", config))
    flyway.setLocations("filesystem:./sql")
    flyway.clean()

    flyway.migrate()
  }

  private def jdbcDataSourceFromConfig(section: String, config: com.typesafe.config.Config) = {
    val source = new PGSimpleDataSource

    source.setServerName(config.getString(s"$section.host"))
    source.setUser(config.getString(s"$section.user"))
    source.setDatabaseName(config.getString(s"$section.name"))

    source

  }
}
