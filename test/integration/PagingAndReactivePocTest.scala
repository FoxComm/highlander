import akka.http.scaladsl.model.StatusCodes

import models.{Customer, Customers}
import org.scalatest.mock.MockitoSugar
import util.IntegrationTestBase
import utils.Slick.implicits._

class PagingAndReactivePocTest extends IntegrationTestBase
with HttpSupport
with AutomaticAuth
with MockitoSugar {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "Paging POC" - {

    "shows a list of customers w/o any pagination" in new Fixture {
      val response = GET(s"v1/customersPaged")
      response.status must === (StatusCodes.OK)
      parse(response.bodyText).extract[Seq[Customer]] must === (customers)
    }

    "shows a list of customers with pagination" in new Fixture {
      val response = GET(s"v1/customersPaged?pageSize=5&pageNo=3")
      response.status must === (StatusCodes.OK)
      parse(response.bodyText).extract[Seq[Customer]] must === (customers.drop(10).take(5))
    }

  }


  "Reactive infinite scrolling POC" - {

    "shows a full list of customers" in new Fixture {
      val response = GET(s"v1/customersReactive")
      response.status must === (StatusCodes.OK)
      parse(response.bodyText).extract[Seq[Customer]] must === (customers)
    }

    "shows a list of customers starting from 10" in new Fixture {
      val response = GET(s"v1/customersReactive?startFrom=10")
      response.status must === (StatusCodes.OK)
      parse(response.bodyText).extract[Seq[Customer]] must === (customers.drop(9))
    }

  }


  trait Fixture {
    val numberOfCustomers = 50

    def generateCustomer(i: Int): Customer = {
      Customer(email = s"email$i@yax.com", password = "password",
        firstName = s"firstName$i", lastName = s"lastName$i")
    }

    val customers = (1 to numberOfCustomers).map { i ⇒
      Customers.save(generateCustomer(i)).run().futureValue
    }
  }
}

