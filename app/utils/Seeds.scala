package utils

import com.typesafe.config.ConfigFactory
import models._
import org.flywaydb.core.Flyway
import org.joda.time.DateTime
import org.postgresql.ds.PGSimpleDataSource
import slick.dbio
import slick.dbio.Effect.{All, Write}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

import utils.Money.Currency

object Seeds {
  val today = new DateTime

  final case class TheWorld(customer: Customer, order: Order, orderNotes: Seq[Note], address: Address, cc: CreditCard,
    storeAdmin: StoreAdmin, shippingAddresses: Seq[OrderShippingAddress], shippingMethods: Seq[ShippingMethod],
    shippingPriceRules: Seq[ShippingPriceRule], shippingMethodRuleMappings: Seq[ShippingMethodPriceRule],
    orderCriteria: Seq[OrderCriterion], orderPriceCriteria: Seq[OrderPriceCriterion],
    priceRuleCriteriaMappings: Seq[ShippingPriceRuleOrderCriterion], skus: Seq[Sku],
    orderLineItems: Seq[OrderLineItem], shipment: Shipment)

  final case class PaymentMethods(giftCard: GiftCard = Factories.giftCard, storeCredit: StoreCredit = Factories.storeCredit)

  def run()(implicit db: Database): dbio.DBIOAction[(Customer, Order, Address, OrderShippingAddress, CreditCard),
    NoStream, Write with Write with Write with All with Write with Write with All with All with Write with All with
    Write with Write with Write with Write with Write with All] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val s = TheWorld(
      customer = Factories.customer,
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
      orderCriteria = Factories.orderCriteria,
      orderPriceCriteria = Factories.orderPriceCriteria,
      priceRuleCriteriaMappings = Factories.priceRuleCriteriaMappings,
      orderLineItems = Factories.orderLineItems,
      shipment = Factories.shipment
    )

    val failures = List(s.customer.validate, s.storeAdmin.validate, s.order.validate, s.address.validate, s.cc.validate).
      filterNot(_.isValid)

    if (failures.nonEmpty)
      throw new Exception(failures.map(_.messages).mkString("\n"))

    for {
      customer ← (Customers.returningId += s.customer).map(id => s.customer.copy(id = id))
      storeAdmin ← (StoreAdmins.returningId += s.storeAdmin).map(id => s.storeAdmin.copy(id = id))
      skus ←  Skus ++= s.skus
      order ← Orders._create(s.order.copy(customerId = customer.id))
      orderNotes ← Notes ++= s.orderNotes
      orderLineItem ← OrderLineItems ++= s.orderLineItems
      address ← Addresses.save(s.address.copy(customerId = customer.id))
      shippingAddress ← OrderShippingAddresses.save(Factories.shippingAddress.copy(orderId = order.id))
      shippingMethods ← ShippingMethods ++= s.shippingMethods
      creditCard ← CreditCards.save(s.cc.copy(customerId = customer.id, billingAddressId = address.id))
      shippingPriceRule ← ShippingPriceRules ++= s.shippingPriceRules
      shippingMethodRuleMappings ← ShippingMethodsPriceRules ++= s.shippingMethodRuleMappings
      orderCriterion ← OrderCriteria ++= s.orderCriteria
      orderPriceCriterion ← OrderPriceCriteria ++= s.orderPriceCriteria
      priceRuleCriteriaMapping ← ShippingPriceRulesOrderCriteria ++= s.priceRuleCriteriaMappings
      shipments ← Shipments.save(s.shipment)
    } yield (customer, order, address, shippingAddress, creditCard)
  }

  object Factories {
    def customer = Customer(email = "yax@yax.com", password = "password",
      firstName = "Yax", lastName = "Fuentes", phoneNumber = Some("123-444-4388"),
      location = Some("DonkeyVille, TN"), modality = Some("Desktop[PC]"))

    def storeAdmin = StoreAdmin(email = "admin@admin.com", password = "password", firstName = "Frankly", lastName = "Admin")

    def order = Order(customerId = 0, referenceNumber = "ABCD1234-11", status = Order.ManualHold)

    def cart = order.copy(status = Order.Cart)

    def orderNotes: Seq[Note] = Seq(
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = "This customer is a donkey."),
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = "No, seriously."),
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = "Like, an actual donkey."),
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = "How did a donkey even place an order on our website?")
    )

    def orderPayment =
      OrderPayment(paymentMethodId = 1, paymentMethodType = "stripe", appliedAmount = 10, status = "auth",
        responseCode = "ok")

    def skus: Seq[Sku] = Seq(Sku(id = 0, name = Some("Flonkey"), price = 33), Sku(name = Some("Shark"), price = 45), Sku(name = Some("Dolphin"), price = 88))

    def orderLineItems: Seq[OrderLineItem] = Seq(OrderLineItem(id = 0, orderId = 1, skuId = 1, status = OrderLineItem.Cart), OrderLineItem(id = 0, orderId = 1, skuId = 2, status = OrderLineItem.Cart), OrderLineItem(id = 0, orderId = 1, skuId = 3, status = OrderLineItem.Cart))

    def address = Address(customerId = 0, stateId = 1, name = "Home", street1 = "555 E Lake Union St.",
        street2 = None, city = "Seattle", zip = "12345", isDefaultShipping = true)

    def shippingAddress = OrderShippingAddress(stateId = 46, name = "Old Yax", street1 = "9313 Olde Mill Pond Dr",
      street2 = None, city = "Glen Allen", zip = "23060")

    def creditCard =
      CreditCard(customerId = 0, gatewayCustomerId = "", lastFour = "4242",
        expMonth = today.getMonthOfYear, expYear = today.getYear + 2, isDefault = true)

    def reason = Reason(id = 0, storeAdminId = 0, body = "I'm a reason", parentId = None)

    def storeCredit = StoreCredit(customerId = 0, originId = 0, originType = "FIXME", originalBalance = 50,
      currency = Currency.USD)

    def storeCreditManual = StoreCreditManual(adminId = 0, reasonId = 0)

    def giftCard = GiftCard(currency = Currency.USD, originId = 0, originType = "FIXME", code = "ABC-123",
      originalBalance = 50)

    def giftCardManual = GiftCardManual(adminId = 0, reasonId = 0)

    def shippingMethods = Seq(
      ShippingMethod(adminDisplayName = "UPS Ground", storefrontDisplayName = "UPS Ground", defaultPrice = 10, isActive = true),
      ShippingMethod(adminDisplayName = "UPS Next day", storefrontDisplayName = "UPS Next day", defaultPrice = 20, isActive = true),
      ShippingMethod(adminDisplayName = "DHL Express", storefrontDisplayName = "DHL Express", defaultPrice = 25, isActive = true)
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

    def orderCriteria: Seq[OrderCriterion] = Seq(
      OrderCriterion(name = "doesn'tmater"),
      OrderCriterion(name = "doesn'tmater"),
      OrderCriterion(name = "doesn'tmater")
    )

    def orderPriceCriteria = Seq(
      OrderPriceCriterion(id = 1, priceType = OrderPriceCriterion.SubTotal, greaterThan = Some(20), currency = Currency.USD, exclude = false),
      OrderPriceCriterion(id = 2, priceType = OrderPriceCriterion.SubTotal, greaterThan = Some(50), currency = Currency.USD, exclude = false),
      OrderPriceCriterion(id = 3, priceType = OrderPriceCriterion.SubTotal, greaterThan = Some(100), currency = Currency.USD, exclude = false)
    )

    def priceRuleCriteriaMappings = Seq(
      ShippingPriceRuleOrderCriterion(orderCriterionId = 1, shippingPricingRuleId = 1),
      ShippingPriceRuleOrderCriterion(orderCriterionId = 2, shippingPricingRuleId = 2),
      ShippingPriceRuleOrderCriterion(orderCriterionId = 3, shippingPricingRuleId = 3)
    )

    def shipment = Shipment(1, 1, Some(1), Some(1))
  }

  def main(args: Array[String]): Unit = {
    Console.err.println(s"Cleaning DB and running migrations")
    flyWayMigrate()
    Console.err.println(s"Inserting seeds")
    implicit val db = Database.forConfig("db.development")
    Await.result(db.run(run()), 5.second)
  }

  private def flyWayMigrate(): Unit = {
    val flyway = new Flyway
    flyway.setDataSource(jdbcDataSourceFromConfig("db.development"))
    flyway.setLocations("filesystem:./sql")
    flyway.clean()

    flyway.migrate()
  }

  private def jdbcDataSourceFromConfig(section: String) = {
    val config = ConfigFactory.load
    val source = new PGSimpleDataSource

    source.setServerName(config.getString(s"$section.host"))
    source.setUser(config.getString(s"$section.user"))
    source.setDatabaseName(config.getString(s"$section.name"))

    source

  }
}
