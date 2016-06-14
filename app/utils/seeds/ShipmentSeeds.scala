package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.location.Country.unitedStatesId
import models.rules._
import models.shipping.ShippingPriceRule._
import models.shipping._
import org.json4s.jackson.JsonMethods._
import slick.driver.PostgresDriver.api._
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories._

trait ShipmentSeeds {

  type ShippingMethods =
    (ShippingMethod#Id, ShippingMethod#Id, ShippingMethod#Id, ShippingMethod#Id, ShippingMethod#Id)

  def getShipmentRules(implicit db: Database): DbResultT[ShippingMethods] =
    for {
      methods ← * <~ ShippingMethods.findActive.result
    } yield
      methods.seq.toList match {
        case m1 :: m2 :: m3 :: m4 :: m5 :: Nil ⇒ (m1.id, m2.id, m3.id, m4.id, m5.id)
        case _                                 ⇒ ???
      }

  def createShipmentRules: DbResultT[ShippingMethods] =
    for {
      methods ← * <~ ShippingMethods.createAllReturningIds(shippingMethods)
      _       ← * <~ ShippingPriceRules.createAll(shippingPriceRules)
      _       ← * <~ ShippingMethodsPriceRules.createAll(shippingMethodRuleMappings)
    } yield
      methods.seq.toList match {
        case m1 :: m2 :: m3 :: m4 :: m5 :: Nil ⇒ (m1, m2, m3, m4, m5)
        case _                                 ⇒ ???
      }

  def shippingMethods =
    Seq(
        ShippingMethod(adminDisplayName = "UPS Ground",
                       storefrontDisplayName = "UPS Ground",
                       price = 250,
                       isActive = true,
                       conditions = Some(upsGround)),
        ShippingMethod(adminDisplayName = "UPS Next day",
                       storefrontDisplayName = "UPS Next day",
                       price = 700,
                       isActive = true,
                       conditions = Some(over50Bucks)),
        ShippingMethod(adminDisplayName = "UPS 2-day",
                       storefrontDisplayName = "UPS 2-day",
                       price = 550,
                       isActive = true,
                       conditions = Some(upsGround)),
        ShippingMethod(adminDisplayName = "DHL Express",
                       storefrontDisplayName = "DHL Express",
                       price = 1500,
                       isActive = true,
                       conditions = Some(under50Bucks)),
        ShippingMethod(adminDisplayName = "International",
                       storefrontDisplayName = "International",
                       price = 1000,
                       isActive = true,
                       conditions = Some(international))
    )

  def shippingPriceRules = Seq(
      ShippingPriceRule(
          name = "Flat Shipping Over 20", ruleType = Flat, flatPrice = 10000, flatMarkup = 0),
      ShippingPriceRule(
          name = "Flat Shipping Over 50", ruleType = Flat, flatPrice = 5000, flatMarkup = 0),
      ShippingPriceRule(
          name = "Flat Shipping Over 100", ruleType = Flat, flatPrice = 1000, flatMarkup = 0)
  )

  def shippingMethodRuleMappings = Seq(
      ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 1, ruleRank = 1),
      ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 2, ruleRank = 2),
      ShippingMethodPriceRule(shippingMethodId = 1, shippingPriceRuleId = 3, ruleRank = 3),
      ShippingMethodPriceRule(shippingMethodId = 2, shippingPriceRuleId = 1, ruleRank = 1),
      ShippingMethodPriceRule(shippingMethodId = 2, shippingPriceRuleId = 2, ruleRank = 2),
      ShippingMethodPriceRule(shippingMethodId = 2, shippingPriceRuleId = 3, ruleRank = 3),
      ShippingMethodPriceRule(shippingMethodId = 4, shippingPriceRuleId = 1, ruleRank = 1)
  )

  def upsGround = parse(s"""
    | {
    |   "comparison": "and",
    |   "conditions": [{
    |     "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": $unitedStatesId
    |   }]
    | }
  """.stripMargin).extract[QueryStatement]

  def international = parse(s"""
    | {
    |   "comparison": "and",
    |   "conditions": [{
    |     "rootObject": "ShippingAddress", "field": "countryId", "operator": "notEquals", "valInt": $unitedStatesId
    |   }]
    | }
  """.stripMargin).extract[QueryStatement]

  val canadaId = 39

  def over50Bucks = parse(s"""
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

  def under50Bucks = parse(s"""
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

  def condition =
    Condition(
        rootObject = "Order", field = "subtotal", operator = Condition.Equals, valInt = Some(50))
}
