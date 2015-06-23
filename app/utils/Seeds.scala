package utils

import models._

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

object Seeds {
  val today = new DateTime

  case class TheWorld(customer: Customer,order: Order, address: Address,
                      storeAdmin: StoreAdmin, shippingMethod: ShippingMethod,
                       shippingPriceRule: ShippingPriceRule, shippingMethodRuleMapping: ShippingMethodPriceRule,
                       orderCriterion: OrderCriterion, orderPriceCriterion: OrderPriceCriterion,
                       priceRuleCriteriaMapping: ShippingPriceRuleOrderCriterion)

  def run(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val db = Database.forURL("jdbc:postgresql://localhost/phoenix_development?user=phoenix", driver = "slick.driver.PostgresDriver")

    val s = TheWorld(
      customer = Factories.customer,
      storeAdmin = Factories.storeAdmin,
      order = Factories.order,
      address = Factories.address,
      shippingMethod = Factories.shippingMethod,
      shippingPriceRule = Factories.shippingPriceRule,
      shippingMethodRuleMapping = Factories.shippingMethodRuleMapping,
      orderCriterion = Factories.orderCriterion,
      orderPriceCriterion = Factories.orderPriceCriterion,
      priceRuleCriteriaMapping = Factories.priceRuleCriteriaMapping
    )

    val failures = List(s.customer.validate, s.storeAdmin.validate, s.order.validate, s.address.validate).
      filterNot(_.isValid)

    if (failures.nonEmpty)
      throw new Exception(failures.map(_.messages).mkString("\n"))

    val actions = for {
      customer ← (Customers.returningId += s.customer).map(id => s.customer.copy(id = id))
      storeAdmin ← (StoreAdmins.returningId += s.storeAdmin).map(id => s.storeAdmin.copy(id = id))
      order ← Orders.save(s.order.copy(customerId = customer.id))
      address ← Addresses.save(s.address.copy(customerId = customer.id))
      shippingMethod ← ShippingMethods.save(s.shippingMethod)
      shippingPriceRule ← ShippingPriceRules.save(s.shippingPriceRule)
      shippingMethodRuleMapping ← ShippingMethodsPriceRules.save(s.shippingMethodRuleMapping)
    } yield (customer, order, address)

    Await.result(actions.run(), 1.second)
  }

  object Factories {
    def customer = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")

    def storeAdmin = StoreAdmin(email = "admin@admin.com", password = "password", firstName = "Frankly", lastName = "Admin")


    def order = Order(customerId = 0)

    def address =
      Address(customerId = 0, stateId = 1, name = "Home", street1 = "555 E Lake Union St.",
        street2 = None, city = "Seattle", zip = "12345")


    def shippingMethod = ShippingMethod(adminDisplayName = "UPS Ground", storefrontDisplayName = "UPS Ground", defaultPrice = 10, isActive = true)

    def shippingPriceRule = ShippingPriceRule(name = "Flat Shipping Over 20", ruleType = ShippingPriceRule.Flat, flatPrice = 99938, flatMarkup = 0)

    def shippingMethodRuleMapping = ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 1, ruleRank = 1)

    def orderCriterion = OrderCriterion(name = "doesn'tmater")

    def orderPriceCriterion = OrderPriceCriterion(id = 1, priceType = OrderPriceCriterion.SubTotal, greaterThan = Some(20), currency = "USD", exclude = false)

    def priceRuleCriteriaMapping = ShippingPriceRuleOrderCriterion(orderCriterionId = 1, shippingPricingRuleId = 1)

  }
}
