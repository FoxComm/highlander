package utils

import models.GiftCard._
import models.{Orders, GiftCards}
import slick.driver.PostgresDriver.api._
import util.TestBase

class QueryErrorInfoTest extends TestBase {

  "Query error info" - {

    "is generated correctly for level 1 query (order)" in {
      val q = Orders.findByRefNum("foobar")

      val result = QueryErrorInfo.forQuery(q)
      result.modelType must === ("order")
      result.searchKey must === ("foobar")
      result.searchTerm must === ("referenceNumber")
    }

    "is generated correctly for level 1 query (gc)" in {
      val q = GiftCards.findByCode("foobar")

      val result = QueryErrorInfo.forQuery(q)
      result.modelType must === ("giftCard")
      result.searchKey must === ("foobar")
      result.searchTerm must === ("code")
    }

    "is generated correctly for level 2 query" in {
      val q = GiftCards.findByCode("foobar").filter(_.status === (Active: Status))

      val result = QueryErrorInfo.forQuery(q)
      result.modelType must === ("giftCard")
      result.searchKey must === ("foobar")
      result.searchTerm must === ("code")
    }

    "is generated correctly for level 3 query" in {
      val q = GiftCards.findByCode("foobar").filter(_.status === (Active: Status)).filter(_.originalBalance === 100)

      val result = QueryErrorInfo.forQuery(q)
      result.modelType must === ("giftCard")
      result.searchKey must === ("foobar")
      result.searchTerm must === ("code")
    }
  }
}
