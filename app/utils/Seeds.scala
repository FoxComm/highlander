package utils

import com.typesafe.config.ConfigFactory
import models._
import org.flywaydb.core.Flyway
import org.joda.time.DateTime
import org.postgresql.ds.PGSimpleDataSource
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

object Seeds {
  val today = new DateTime

  case class TheWorld(customer: Customer,order: Order, address: Address, cc: CreditCardGateway,
                      storeAdmin: StoreAdmin, shippingMethod: ShippingMethod,
                       shippingPriceRule: ShippingPriceRule, shippingMethodRuleMapping: ShippingMethodPriceRule,
                       orderCriterion: OrderCriterion, orderPriceCriterion: OrderPriceCriterion,
                       priceRuleCriteriaMapping: ShippingPriceRuleOrderCriterion, skus: Seq[Sku],
                       orderLineItems: Seq[OrderLineItem])

  def run(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val db = Database.forConfig("db.development")

    val s = TheWorld(
      customer = Factories.customer,
      storeAdmin = Factories.storeAdmin,
      skus = Factories.skus,
      order = Factories.order,
      address = Factories.address,
      shippingMethod = Factories.shippingMethod,
      cc = Factories.creditCard,
      shippingPriceRule = Factories.shippingPriceRule,
      shippingMethodRuleMapping = Factories.shippingMethodRuleMapping,
      orderCriterion = Factories.orderCriterion,
      orderPriceCriterion = Factories.orderPriceCriterion,
      priceRuleCriteriaMapping = Factories.priceRuleCriteriaMapping,
      orderLineItems = Factories.orderLineItems
    )

    val failures = List(s.customer.validate, s.storeAdmin.validate, s.order.validate, s.address.validate, s.cc.validate).
      filterNot(_.isValid)

    if (failures.nonEmpty)
      throw new Exception(failures.map(_.messages).mkString("\n"))

    val actions = for {
      customer ← (Customers.returningId += s.customer).map(id => s.customer.copy(id = id))
      storeAdmin ← (StoreAdmins.returningId += s.storeAdmin).map(id => s.storeAdmin.copy(id = id))
      skus ←  Skus ++= s.skus
      order ← Orders.save(s.order.copy(customerId = customer.id))
      orderLineItem ← OrderLineItems ++= s.orderLineItems
      address ← Addresses.save(s.address.copy(customerId = customer.id))
      shippingMethod ← ShippingMethods.save(s.shippingMethod)
      gateway ← CreditCardGateways.save(s.cc.copy(customerId = customer.id))
      shippingPriceRule ← ShippingPriceRules.save(s.shippingPriceRule)
      shippingMethodRuleMapping ← ShippingMethodsPriceRules.save(s.shippingMethodRuleMapping)
    } yield (customer, order, address, gateway)

    Await.result(actions.run(), 1.second)
  }

  object Factories {
    def customer = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")

    def storeAdmin = StoreAdmin(email = "admin@admin.com", password = "password", firstName = "Frankly", lastName = "Admin")


    def order = Order(customerId = 0)

    def skus: Seq[Sku] = Seq(Sku(id = 0, name = Some("Flonkey"), price = 33), Sku(name = Some("Shark"), price = 45), Sku(name = Some("Dolphin"), price = 88))

    def orderLineItems: Seq[OrderLineItem] = Seq(OrderLineItem(id = 0, orderId = 1, skuId = 1, status = OrderLineItem.Cart), OrderLineItem(id = 0, orderId = 1, skuId = 2, status = OrderLineItem.Cart), OrderLineItem(id = 0, orderId = 1, skuId = 3, status = OrderLineItem.Cart))

    def address =
      Address(customerId = 0, stateId = 1, name = "Home", street1 = "555 E Lake Union St.",
        street2 = None, city = "Seattle", zip = "12345")

    def creditCard =
      CreditCardGateway(customerId = 0, gatewayCustomerId = "", lastFour = "4242",
        expMonth = today.getMonthOfYear, expYear = today.getYear + 2)

    def shippingMethod = ShippingMethod(adminDisplayName = "UPS Ground", storefrontDisplayName = "UPS Ground", defaultPrice = 10, isActive = true)

    def shippingPriceRule = ShippingPriceRule(name = "Flat Shipping Over 20", ruleType = ShippingPriceRule.Flat, flatPrice = 99938, flatMarkup = 0)

    def shippingMethodRuleMapping = ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 1, ruleRank = 1)

    def orderCriterion = OrderCriterion(name = "doesn'tmater")

    def orderPriceCriterion = OrderPriceCriterion(id = 1, priceType = OrderPriceCriterion.SubTotal, greaterThan = Some(20), currency = "USD", exclude = false)

    def priceRuleCriteriaMapping = ShippingPriceRuleOrderCriterion(orderCriterionId = 1, shippingPricingRuleId = 1)
  }

  def main(args: Array[String]) {
    Console.err.println(s"Cleaning DB and running migrations")
    flyWayMigrate()
    Console.err.println(s"Inserting seeds")
    run()
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
