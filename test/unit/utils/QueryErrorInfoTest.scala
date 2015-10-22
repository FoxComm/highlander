package utils

import models.GiftCard._
import models.{Orders, GiftCards}
import slick.driver.PostgresDriver.api._
import util.TestBase

class QueryErrorInfoTest extends TestBase {

  "Query error info" - {

    "is generated correctly for level 1 query (order)" in {
      val q = Orders.findByRefNum("foobar")

      QueryErrorInfo.searchKeyForQuery(q, Orders.primarySearchTerm).value must === ("foobar")
    }

    "is generated correctly for level 1 query (gc)" in {
      val q = GiftCards.findByCode("foobar")

      QueryErrorInfo.searchKeyForQuery(q, GiftCards.primarySearchTerm).value must === ("foobar")
    }

    "is generated correctly for level 2 query" in {
      val q = GiftCards.findByCode("foobar").filter(_.status === (Active: Status))

      QueryErrorInfo.searchKeyForQuery(q, GiftCards.primarySearchTerm).value must === ("foobar")
    }

    "is generated correctly for level 2 query with swapped terms" in {
      val q = GiftCards.filter(_.status === (Active: Status)).filter(_.code === "foobar")

      QueryErrorInfo.searchKeyForQuery(q, GiftCards.primarySearchTerm).value must === ("foobar")
    }

    "is generated correctly for level 3 query" in {
      val q = GiftCards.findByCode("foobar").filter(_.status === (Active: Status)).filter(_.originalBalance === 100)

      QueryErrorInfo.searchKeyForQuery(q, GiftCards.primarySearchTerm).value must === ("foobar")
    }

    "is generated correctly for level 3 query with swapped terms" in {
      val q = GiftCards.filter(_.status === (Active: Status)).filter(_.originalBalance === 100).filter(_.code === "foobar")

      QueryErrorInfo.searchKeyForQuery(q, GiftCards.primarySearchTerm).value must === ("foobar")
    }
  }
}
