package responses

import concurrent.ExecutionContext.Implicits.global
import models.Order
import org.joda.time.DateTime
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.{write ⇒ render}
import util.IntegrationTestBase
import utils.{JsonFormatters, Seeds}

class AllOrdersIntegrationTest extends IntegrationTestBase {

  implicit val formats = JsonFormatters.phoenixFormats

  "AllOrders" - {
    "findAll" in {
      db.run(Seeds.run()).futureValue
      val json = render(AllOrders.findAll.futureValue)
      val root = parse(json)

      (root \ "referenceNumber").extract[String] must === ("ABCD1234-11")
      (root \ "email").extract[String] must === ("yax@yax.com")
      (root \ "orderStatus").extract[Order.Status] must === (Order.ManualHold)
      (root \ "total").extract[Int] must === (27)
      (root \ "paymentStatus").extract[Option[String]] must === (None)
      (root \ "placedAt").extract[Option[DateTime]] must === (None)
    }
  }
}
