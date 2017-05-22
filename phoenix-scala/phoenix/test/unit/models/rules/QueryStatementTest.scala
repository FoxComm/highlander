package models.rules

import org.json4s._
import org.json4s.jackson.JsonMethods._
import phoenix.models.rules.{Condition, QueryStatement}
import phoenix.utils.JsonFormatters._
import testutils.TestBase

class QueryStatementTest extends TestBase {
  implicit val formats = phoenixFormats

  "QueryStatement" - {
    "JSON deserialization" - {
      "domestic shipping to non-P.0. boxes from JSON" in {
        val json = """
            | {
            |   "comparison": "and",
            |   "conditions": [
            |     {
            |       "rootObject": "ShippingAddress",
            |       "field": "address1",
            |       "operator": "notContains",
            |       "valString": "p.o. box"
            |     },
            |     {
            |       "rootObject": "ShippingAddress",
            |       "field": "countryId",
            |       "operator": "equals",
            |       "valInt": 1
            |     }
            |   ]
            | }
          """.stripMargin

        val statement = parse(json).extract[QueryStatement]
        statement.comparison must === (QueryStatement.And)

        val condition1 :: condition2 :: Nil = statement.conditions
        condition1.rootObject must === ("ShippingAddress")
        condition1.field must === ("address1")
        condition1.operator must === (Condition.NotContains)
        condition1.valString must === (Some("p.o. box"))

        condition2.rootObject must === ("ShippingAddress")
        condition2.field must === ("countryId")
        condition2.operator must === (Condition.Equals)
        condition2.valInt must === (Some(1))
      }

      "order subtotal is greater than $50 and less than $200" in {
        val json = """
            | {
            |   "comparison": "and",
            |   "conditions": [
            |     {
            |       "rootObject": "Order",
            |       "field": "subtotal",
            |       "operator": "greaterThanOrEquals",
            |       "valInt": 50
            |     },
            |     {
            |       "rootObject": "Order",
            |       "field": "subtotal",
            |       "operator": "lessThan",
            |       "valInt": 200
            |     }
            |   ]
            | }
          """.stripMargin

        val statement = parse(json).extract[QueryStatement]
        statement.comparison must === (QueryStatement.And)

        val condition1 :: condition2 :: Nil = statement.conditions
        condition1.rootObject must === ("Order")
        condition1.field must === ("subtotal")
        condition1.operator must === (Condition.GreaterThanOrEquals)
        condition1.valInt must === (Some(50))

        condition2.rootObject must === ("Order")
        condition2.field must === ("subtotal")
        condition2.operator must === (Condition.LessThan)
        condition2.valInt must === (Some(200))
      }
    }
  }
}
