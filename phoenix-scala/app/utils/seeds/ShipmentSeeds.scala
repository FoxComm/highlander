package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.location.Country.unitedStatesId
import models.rules._
import models.shipping._
import org.json4s.jackson.JsonMethods._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.seeds.Seeds.Factories._
import models.shipping.ShippingMethod._

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
    } yield
      methods.seq.toList match {
        case m1 :: m2 :: m3 :: m4 :: Nil ⇒ (m1, m2, m3, m4)
        case _                           ⇒ ???
      }

  def shippingMethods =
    Seq(
        ShippingMethod(name = standardShippingName,
                       code = standardShippingCode,
                       price = 300,
                       isActive = true,
                       conditions = Some(under50Bucks)),
        ShippingMethod(name = standardShippingName,
                       code = standardShippingFreeCode,
                       price = 0,
                       isActive = true,
                       conditions = Some(over50Bucks)),
        ShippingMethod(name = expressShippingName,
                       code = expressShippingCode,
                       price = 1500,
                       isActive = true,
                       conditions = Some(usOnly)),
        ShippingMethod(name = overnightShippingName,
                       code = overnightShippingCode,
                       price = 3000,
                       isActive = true,
                       conditions = Some(usOnly))
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
