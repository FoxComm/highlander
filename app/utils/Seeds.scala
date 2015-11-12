package utils

import java.time.{ZoneId, Instant}
import java.time.temporal.ChronoField

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

import models._
import models.rules._
import org.flywaydb.core.Flyway
import org.json4s.jackson.JsonMethods._
import org.postgresql.ds.PGSimpleDataSource
import slick.dbio
import slick.dbio.Effect.{All, Write}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.flyway.newFlyway

object Seeds {

  def main(args: Array[String]): Unit = {
    Console.err.println(s"Cleaning DB and running migrations")
    val config: com.typesafe.config.Config = utils.Config.loadWithEnv()
    flyWayMigrate(config)
    Console.err.println(s"Inserting seeds")
    implicit val db: PostgresDriver.backend.DatabaseDef = Database.forConfig("db", config)
    Await.result(db.run(run()), 5.second)

    args.headOption.map {
      case "ranking" ⇒
        Console.err.println(s"Inserting ranking seeds")
        Await.result(db.run(insertRankingSeeds(1700).transactionally), 7.second)
      case _ ⇒ None
    }
  }

  val today = Instant.now().atZone(ZoneId.of("UTC"))

  def randomOrderStatus: Order.Status = {
    val types = Order.Status.types.filterNot(_ == Order.Cart)
    val index = Random.nextInt(types.size)
    types.drop(index).head
  }

  final case class TheWorld(customers: Seq[Customer], order: Order, orderNotes: Seq[Note], address: Address,
    cc: CreditCard, storeAdmin: StoreAdmin, shippingAddresses: Seq[OrderShippingAddress],
    shippingMethods: Seq[ShippingMethod], shippingPriceRules: Seq[ShippingPriceRule],
    shippingMethodRuleMappings: Seq[ShippingMethodPriceRule], skus: Seq[Sku], orderLineItems: Seq[OrderLineItem],
    orderPayments: Seq[OrderPayment], shipment: Shipment, paymentMethods: AllPaymentMethods, reasons: Seq[Reason],
    orderLineItemSkus: Seq[OrderLineItemSku], inventorySummaries: Seq[InventorySummary],
    gcSubTypes: Seq[GiftCardSubtype], scSubTypes: Seq[StoreCreditSubtype], rmaReasons: Seq[RmaReason], rma: Rma,
    rmaLineItems: Seq[RmaLineItem], rmaLineItemSkus: Seq[RmaLineItemSku], rmaNotes: Seq[Note])

  final case class AllPaymentMethods(giftCard: GiftCard = Factories.giftCard, storeCredit: StoreCredit = Factories
    .storeCredit)

  def insertRankingSeeds(customersCount: Int)(implicit db: Database) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    import Factories.{generateCustomer, generateOrder, generateOrderPayment}

    val location = "Arkham"

    def makeOrders(c: Customer) = {
      (1 to 5 + Random.nextInt(20)).map { i ⇒ generateOrder(Order.Shipped, c.id) }
    }

    def makePayment(o: Order, pm: CreditCard) = {
      generateOrderPayment(o.id, pm, Random.nextInt(20000) + 100)
    }

    val insertCustomers = Customers ++=  (1 to customersCount).map { i ⇒
      val s = Factories.randomString(15)
      Customer(name = Some(s), email = s"${s}-${i}@email.com", password = Some(s), location = Some(location))
    }

    val insertOrders = Customers.filter(_.location === location).result.flatMap {
      case customers ⇒
        DBIO.sequence(Seq(
          CreditCards ++= customers.map { c ⇒ Factories.creditCard.copy(customerId = c.id,
            holderName = c.name.getOrElse(""))
          },
          Orders ++= customers.flatMap(makeOrders)))
    }

    val insertPayments = {
      val action = (for {
        (o, cc) ← Orders.join(CreditCards).on(_.customerId === _.customerId)
      } yield (o, cc)).result

      action.flatMap { ordersWithCc ⇒
          OrderPayments ++= ordersWithCc.map { c ⇒ makePayment(c._1, c._2) }
      }
    }

    DBIO.sequence(Seq(insertCustomers, insertOrders, insertPayments))
  }

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
      inventorySummaries = Factories.inventorySummaries,
      gcSubTypes = Factories.giftCardSubTypes,
      scSubTypes = Factories.storeCreditSubTypes,
      rmaReasons = Factories.rmaReasons,
      rma = Factories.rma,
      rmaLineItemSkus = Factories.rmaLineItemSkus,
      rmaLineItems = Factories.rmaLineItems,
      rmaNotes = Factories.rmaNotes
    )

    s.address.validate.fold(err ⇒ throw new Exception(err.mkString("\n")), _ ⇒ {})
    s.storeAdmin.validate.fold(err ⇒ throw new Exception(err.mkString("\n")), _ ⇒ {})
    s.order.validate.fold(err ⇒ throw new Exception(err.mkString("\n")), _ ⇒ {})
    s.cc.validate.fold(err ⇒ throw new Exception(err.mkString("\n")), _ ⇒ {})

    val failures = s.customers.map { _.validate }.filterNot(_.isValid)

    if (failures.nonEmpty)
      throw new Exception(failures.map(_.mkString("\n")).mkString("\n"))

    for {
      customer ← (Customers.returningId += Factories.customer).map(id ⇒ Factories.customer.copy(id = id))
      customers ← Customers ++= s.customers
      storeAdmin ← (StoreAdmins.returningId += s.storeAdmin).map(id ⇒ s.storeAdmin.copy(id = id))
      skus ← Skus ++= s.skus
      summaries ← InventorySummaries ++= s.inventorySummaries
      order ← Orders.saveNew(s.order.copy(customerId = customer.id))
      orderNotes ← Notes ++= s.orderNotes
      orderLineItemOrigins ← OrderLineItemSkus ++= s.orderLineItemSkus
      orderLineItem ← OrderLineItems ++= s.orderLineItems
      address ← Addresses.saveNew(s.address.copy(customerId = customer.id))
      shippingAddress ← OrderShippingAddresses.saveNew(Factories.shippingAddress.copy(orderId = order.id))
      shippingMethods ← ShippingMethods ++= s.shippingMethods
      creditCard ← CreditCards.saveNew(s.cc.copy(customerId = customer.id))
      orderPayment ← OrderPayments.saveNew(Factories.orderPayment.copy(orderId = order.id,
        paymentMethodId = creditCard.id, amount = Some(100)))
      shippingPriceRule ← ShippingPriceRules ++= s.shippingPriceRules
      shippingMethodRuleMappings ← ShippingMethodsPriceRules ++= s.shippingMethodRuleMappings
      orderShippingMethod ← OrderShippingMethods.saveNew(OrderShippingMethod(orderId = order.id, shippingMethodId = 1))
      shipments ← Shipments.saveNew(s.shipment.copy(orderShippingMethodId = Some(orderShippingMethod.id)))
      reasons ← Reasons ++= s.reasons.map(_.copy(storeAdminId = storeAdmin.id))
      gcSubTypes ← GiftCardSubtypes ++= s.gcSubTypes
      scSubTypes ← StoreCreditSubtypes ++= s.scSubTypes
      gcOrigin ← GiftCardManuals.saveNew(Factories.giftCardManual.copy(adminId = storeAdmin.id, reasonId = 1))
      giftCard ← GiftCards.saveNew(s.paymentMethods.giftCard.copy(originId = gcOrigin.id))
      gcAdjustments ← GiftCards.auth(giftCard, Some(orderPayment.id), 10)
      scOrigin ← StoreCreditManuals.saveNew(Factories.storeCreditManual.copy(adminId = storeAdmin.id, reasonId = 1))
      storeCredit ← StoreCredits.saveNew(s.paymentMethods.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
      storeCreditAdjustments ← StoreCredits.auth(storeCredit, Some(orderPayment.id), 10)
      rmaReasons ← RmaReasons ++= s.rmaReasons
      rma ← Rmas.saveNew(s.rma.copy(customerId = customer.id))
      rmaLineItemSkus ← RmaLineItemSkus ++= s.rmaLineItemSkus
      rmaLineItems ← RmaLineItems ++= s.rmaLineItems
      rmaNotes ← Notes ++= s.rmaNotes
    } yield (customers, order, address, shippingAddress, creditCard, giftCard, storeCredit)
  }

  object Factories {
    implicit val formats = JsonFormatters.phoenixFormats

    def randomString(len: Int) = Random.alphanumeric.take(len).mkString.toLowerCase

    def customer = Customer(email = "yax@yax.com", password = Some("password"),
      name = Some("Yax Fuentes"), phoneNumber = Some("123-444-4388"),
      location = Some("DonkeyVille, TN"), modality = Some("Desktop[PC]"))

    def customers: Seq[Customer] = Seq(
      Customer(email = "adil@adil.com", password = Some("password"),
        name = Some("Adil Wali"), phoneNumber = Some("123-444-0909"),
        location = Some("DonkeyHill, WA"), modality = Some("Desktop[PC]")),

      Customer(email = "tivs@tivs.com", password = Some("password"),
        name = Some("Jonathan Rainey"), phoneNumber = Some("858-867-5309"),
        location = Some("DonkeyTown, NY"), modality = Some("Desktop[PC]")),

      Customer(email = "cam@cam.com", password = Some("password"),
        name = Some("Cameron Stitt"), phoneNumber = Some("883-444-4321"),
        location = Some("Donkeysburg, AU"), modality = Some("Desktop[PC]"))
    )

    def generateCustomer: Customer = Customer(email = s"${randomString(10)}@email.com",
      password = Some(randomString(10)), name = Some(randomString(10)))

    def generateOrder(status: Order.Status, customerId: Int): Order = {
      Order(customerId = customerId, referenceNumber = randomString(8) + "-17", status = status)
    }

    def generateOrderPayment[A <: PaymentMethod with ModelWithIdParameter[A]](orderId: Int,
      paymentMethod: A, amount: Int = 100): OrderPayment = {
      orderPayment.copy(orderId = orderId, amount = Some(amount),
        paymentMethodId = paymentMethod.id)
    }

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

    def generateAddress: Address = Address(customerId = 0, regionId = 4177, name = randomString(10),
      address1 = randomString(30), address2 = None, city = "Seattle", zip = "12345", isDefaultShipping = false,
      phoneNumber = None)

    def shippingAddress = OrderShippingAddress(regionId = 4174, name = "Old Yax", address1 = "9313 Olde Mill Pond Dr",
      address2 = None, city = "Glen Allen", zip = "23060", phoneNumber = None)

    def creditCard = {
      CreditCard(customerId = 0, gatewayCustomerId = "cus_6uzC8j5doSTWth", gatewayCardId = "", holderName = "Yax", lastFour = "4242",
        expMonth = today.getMonthValue, expYear = today.getYear + 2, isDefault = true,
        regionId = 4129, addressName = "Old Jeff", address1 = "95 W. 5th Ave.", address2 = Some("Apt. 437"),
        city = "San Mateo", zip = "94402")
    }

    def creditCardCharge = CreditCardCharge(creditCardId = creditCard.id, orderPaymentId = orderPayment.id, chargeId = "foo")

    def reason = Reason(id = 0, storeAdminId = 0, body = "I'm a reason", parentId = None)

    def reasons: Seq[Reason] = Seq(
      Reason(body = "Other", parentId = None, storeAdminId = 0),
      Reason(body = "Cancelled by customer request", parentId = None, storeAdminId = 0),
      Reason(body = "Cancelled because duplication", parentId = None, storeAdminId = 0))

    def storeCreditSubTypes: Seq[StoreCreditSubtype] = Seq(
      StoreCreditSubtype(title = "Appeasement Subtype A", originType = StoreCredit.CsrAppeasement),
      StoreCreditSubtype(title = "Appeasement Subtype B", originType = StoreCredit.CsrAppeasement),
      StoreCreditSubtype(title = "Appeasement Subtype C", originType = StoreCredit.CsrAppeasement)
    )

    def storeCredit = StoreCredit(customerId = 0, originId = 0, originType = StoreCredit.CsrAppeasement, originalBalance = 50,
      currency = Currency.USD)

    def storeCreditManual = StoreCreditManual(adminId = 0, reasonId = 0)

    def giftCardSubTypes: Seq[GiftCardSubtype] = Seq(
      GiftCardSubtype(title = "Appeasement Subtype A", originType = GiftCard.CsrAppeasement),
      GiftCardSubtype(title = "Appeasement Subtype B", originType = GiftCard.CsrAppeasement),
      GiftCardSubtype(title = "Appeasement Subtype C", originType = GiftCard.CsrAppeasement)
    )

    def giftCard = GiftCard(currency = Currency.USD, originId = 0, originType = GiftCard.CsrAppeasement,
      originalBalance = 50)

    def giftCardManual = GiftCardManual(adminId = 0, reasonId = 0)

    def giftCardAdjustment = GiftCardAdjustment.build(giftCard, giftCardPayment)

    def shippingMethodCondition = parse(
      """
        | {
        |   "comparison": "and",
        |   "conditions": [{
        |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThanOrEquals", "valInt": 0
        |   }]
        | }
      """.stripMargin).extract[QueryStatement]

    def shippingMethods = Seq(
      ShippingMethod(adminDisplayName = "UPS Ground", storefrontDisplayName = "UPS Ground", price = 10,
        isActive = true, conditions = Some(shippingMethodCondition)),
      ShippingMethod(adminDisplayName = "UPS Next day", storefrontDisplayName = "UPS Next day", price = 20,
        isActive = true, conditions = Some(shippingMethodCondition)),
      ShippingMethod(adminDisplayName = "DHL Express", storefrontDisplayName = "DHL Express", price = 25,
        isActive = true, conditions = Some(shippingMethodCondition))
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

    def rmaReasons = Seq(
      // Return reasons
      RmaReason(name = "Product Return", reasonType = RmaReason.BaseReason, rmaType = Rma.Standard),
      RmaReason(name = "Damaged Product", reasonType = RmaReason.BaseReason, rmaType = Rma.Standard),
      RmaReason(name = "Return to Sender", reasonType = RmaReason.BaseReason, rmaType = Rma.Standard),
      RmaReason(name = "Not Delivered", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
      RmaReason(name = "Foreign Freight Error", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
      RmaReason(name = "Late Delivery", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
      RmaReason(name = "Sales Tax Error", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
      RmaReason(name = "Shipping Charges Error", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
      RmaReason(name = "Wrong Product", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
      RmaReason(name = "Mis-shipment", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
      RmaReason(name = "Failed Capture", reasonType = RmaReason.BaseReason, rmaType = Rma.RestockOnly),
      // Product return codes
      RmaReason(name = "Doesn't fit", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard),
      RmaReason(name = "Don't like", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard),
      RmaReason(name = "Doesn't look like picture", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard),
      RmaReason(name = "Wrong color", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard),
      RmaReason(name = "Not specified", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard)
    )

    def rma = Rma(referenceNumber = "", orderId = 1, orderRefNum = order.referenceNumber, rmaType = Rma.Standard,
      status = Rma.Pending, customerId = 1)

    def rmaLineItemSkus = Seq(
      RmaLineItemSku(id = 0, rmaId = 1, orderLineItemSkuId = 1),
      RmaLineItemSku(id = 0, rmaId = 1, orderLineItemSkuId = 2)
    )

    def rmaLineItems = Seq(
      RmaLineItem(id = 0, rmaId = 1, reasonId = 12, originId = 1, originType = RmaLineItem.SkuItem,
        rmaType = Rma.Standard, status = Rma.Pending, inventoryDisposition = RmaLineItem.Putaway),
      RmaLineItem(id = 0, rmaId = 1, reasonId = 12, originId = 2, originType = RmaLineItem.SkuItem,
        rmaType = Rma.Standard, status = Rma.Pending, inventoryDisposition = RmaLineItem.Putaway)
    )

    def rmaNotes: Seq[Note] = Seq(
      Note(referenceId = 1, referenceType = Note.Rma, storeAdminId = 1, body = "This customer is a donkey."),
      Note(referenceId = 1, referenceType = Note.Rma, storeAdminId = 1, body = "No, seriously."),
      Note(referenceId = 1, referenceType = Note.Rma, storeAdminId = 1, body = "Like, an actual donkey."),
      Note(referenceId = 1, referenceType = Note.Rma, storeAdminId = 1, body = "How did a donkey even place an order " +
        "on our website?")
    )
  }

  private def flyWayMigrate(config: com.typesafe.config.Config): Unit = {
    val flyway = newFlyway(jdbcDataSourceFromConfig("db", config))

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
