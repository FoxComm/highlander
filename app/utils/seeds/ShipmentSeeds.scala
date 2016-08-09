package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.location.Country.unitedStatesId
import models.rules._
import models.shipping.ShippingPriceRule._
import models.shipping._
import org.json4s.jackson.JsonMethods._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.seeds.Seeds.Factories._

trait ShipmentSeeds {

  type ShippingMethods =
    (ShippingMethod#Id, ShippingMethod#Id, ShippingMethod#Id, ShippingMethod#Id)

  def getShipmentRules(implicit db: DB): DbResultT[ShippingMethods] =
    for {
      methods ← * <~ ShippingMethods.findActive.result
    } yield
      methods.seq.toList match {
        case m1 :: m2 :: m3 :: m4 :: Nil ⇒ (m1.id, m2.id, m3.id, m4.id)
        case _                           ⇒ ???
      }

  def createShipmentRules: DbResultT[ShippingMethods] =
    for {
      methods ← * <~ ShippingMethods.createAllReturningIds(shippingMethods)
      _       ← * <~ ShippingPriceRules.createAll(shippingPriceRules)
      _       ← * <~ ShippingMethodsPriceRules.createAll(shippingMethodRuleMappings)
    } yield
      methods.seq.toList match {
        case m1 :: m2 :: m3 :: m4 :: Nil ⇒ (m1, m2, m3, m4)
        case _                           ⇒ ???
      }

  def shippingMethods =
    Seq(
        ShippingMethod(adminDisplayName = "Standard shipping (USPS)",
                       storefrontDisplayName = "Standard shipping",
                       price = 300,
                       isActive = true,
                       conditions = Some(under50Bucks)),
        ShippingMethod(adminDisplayName = "2-3 day express (FedEx)",
                       storefrontDisplayName = "2-3 day express",
                       price = 1500,
                       isActive = true,
                       conditions = Some(usOnly)),
        ShippingMethod(adminDisplayName = "Overnight (FedEx)",
                       storefrontDisplayName = "Overnight",
                       price = 3000,
                       isActive = true,
                       conditions = Some(usOnly)),
        ShippingMethod(adminDisplayName = "Standard shipping (USPS)",
                       storefrontDisplayName = "Standard shipping",
                       price = 0,
                       isActive = true,
                       conditions = Some(over50Bucks))
    )

  def shippingPriceRules = Seq(
      ShippingPriceRule(name = "Flat Shipping for Standard Delivery",
                        ruleType = Flat,
                        flatPrice = 300,
                        flatMarkup = 0),
      ShippingPriceRule(name = "Flat Shipping for Express Delivery",
                        ruleType = Flat,
                        flatPrice = 1500,
                        flatMarkup = 0),
      ShippingPriceRule(name = "Flat Shipping for Overnight Delivery",
                        ruleType = Flat,
                        flatPrice = 3000,
                        flatMarkup = 0),
      ShippingPriceRule(name = "Flat Shipping Over 50",
                        ruleType = Flat,
                        flatPrice = 5000,
                        flatMarkup = 0)
  )

  def shippingMethodRuleMappings = Seq(
      ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 1, ruleRank = 1),
      ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 4, ruleRank = 2),
      ShippingMethodPriceRule(shippingMethodId = 2, shippingPriceRuleId = 2, ruleRank = 1),
      ShippingMethodPriceRule(shippingMethodId = 3, shippingPriceRuleId = 3, ruleRank = 1)
  )

  def usOnly = parse(s"""
    | {
    |   "comparison": "and",
    |   "conditions": [{
    |     "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
    |   }]
    | }
  """.stripMargin).extract[QueryStatement]

  def over50Bucks = parse(s"""
    | {
    |   "comparison": "and",
    |   "statements": [{
    |     "comparison": "and",
    |     "conditions": [{
    |       "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
    |     }]
    |   }, {
    |     "comparison": "and",
    |     "conditions": [{
    |       "rootObject": "Order", "field": "subtotal", "operator": "greaterThanOrEquals", "valInt": 5000
    |     }]
    |   }]
    | }
  """.stripMargin).extract[QueryStatement]

  def under50Bucks = parse(s"""
     | {
     |   "comparison": "and",
     |   "statements": [{
     |     "comparison": "and",
     |     "conditions": [{
     |       "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
     |     }]
     |   }, {
     |     "comparison": "and",
     |     "conditions": [{
     |       "rootObject": "Order", "field": "subtotal", "operator": "lessThan", "valInt": 5000
     |     }]
     |   }]
     | }
  """.stripMargin).extract[QueryStatement]

  def shipment = Shipment(1, "boo", Some(1), Some(1))

  def condition =
    Condition(rootObject = "Order",
              field = "subtotal",
              operator = Condition.Equals,
              valInt = Some(50))
}
