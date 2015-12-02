package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.Country.unitedStatesId
import models.ShippingPriceRule._
import models.rules.{Condition, QueryStatement}
import models.{Shipment, ShippingMethod, ShippingMethodPriceRule, ShippingMethods, ShippingMethodsPriceRules, ShippingPriceRule, ShippingPriceRules}
import org.json4s.jackson.JsonMethods._
import utils.DbResultT._
import utils.DbResultT.implicits._
import Seeds.Factories._

trait ShipmentSeeds {

  type ShippingMethods = (ShippingMethod#Id, ShippingMethod#Id, ShippingMethod#Id, ShippingMethod#Id)

  def createShipmentRules: DbResultT[ShippingMethods] = for {
    methods ← * <~ ShippingMethods.createAllReturningIds(shippingMethods)
    _ ← * <~ ShippingPriceRules.createAll(shippingPriceRules)
    _ ← * <~ ShippingMethodsPriceRules.createAll(shippingMethodRuleMappings)
  } yield methods.seq.toList match {
      case m1 :: m2 :: m3 :: m4 :: Nil ⇒ (m1, m2, m3, m4)
      case _ ⇒ ???
    }

  def shippingMethods = Seq(
    ShippingMethod(adminDisplayName = "UPS Ground", storefrontDisplayName = "UPS Ground", price = 0,
      isActive = true, conditions = Some(upsGround)),
    ShippingMethod(adminDisplayName = "UPS Next day", storefrontDisplayName = "UPS Next day", price = 0,
      isActive = true, conditions = Some(over50Bucks)),
    ShippingMethod(adminDisplayName = "DHL Express", storefrontDisplayName = "DHL Express", price = 1500,
      isActive = true, conditions = Some(under50Bucks)),
    ShippingMethod(adminDisplayName = "International", storefrontDisplayName = "International", price = 1000,
      isActive = true, conditions = Some(international))
  )

  def shippingPriceRules = Seq(
    ShippingPriceRule(name = "Flat Shipping Over 20", ruleType = Flat, flatPrice = 10000, flatMarkup = 0),
    ShippingPriceRule(name = "Flat Shipping Over 50", ruleType = Flat, flatPrice = 5000, flatMarkup = 0),
    ShippingPriceRule(name = "Flat Shipping Over 100", ruleType = Flat, flatPrice = 1000, flatMarkup = 0)
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

  def upsGround = parse(
  s"""
    | {
    |   "comparison": "and",
    |   "conditions": [{
    |     "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
    |   }]
    | }
  """.stripMargin).extract[QueryStatement]

  def international = parse(
  s"""
    | {
    |   "comparison": "and",
    |   "conditions": [{
    |     "rootObject": "ShippingAddress", "field": "countryId", "operator": "notEquals", "valInt": $unitedStatesId
    |   }]
    | }
  """.stripMargin).extract[QueryStatement]

  val canadaId = 39

  def over50Bucks = parse(
  s"""
    | {
    |   "comparison": "and",
    |   "statements": [{
    |     "comparison": "or",
    |     "conditions": [{
    |       "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
    |     }, {
    |       "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $canadaId
    |     }]
    |   }, {
    |     "comparison": "and",
    |     "conditions": [{
    |       "rootObject": "Order", "field": "subtotal", "operator": "greaterThanOrEquals", "valInt": 5000
    |     }]
    |   }]
    | }
  """.stripMargin).extract[QueryStatement]

  def under50Bucks = parse(
  s"""
     | {
     |   "comparison": "and",
     |   "statements": [{
     |     "comparison": "or",
     |     "conditions": [{
     |       "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
     |     }, {
     |       "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $canadaId
     |     }]
     |   }, {
     |     "comparison": "and",
     |     "conditions": [{
     |       "rootObject": "Order", "field": "subtotal", "operator": "lessThan", "valInt": 5000
     |     }]
     |   }]
     | }
  """.stripMargin).extract[QueryStatement]

  def shipment = Shipment(1, 1, Some(1), Some(1))

  def condition = Condition(rootObject = "Order", field = "subtotal", operator = Condition.Equals, valInt = Some(50))
}
