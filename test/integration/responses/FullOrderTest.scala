package responses

import models.{Orders, Order}

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import org.json4s.DefaultFormats

import org.scalatest.prop.TableDrivenPropertyChecks._
import payloads.CreateAddressPayload
import util.IntegrationTestBase
import utils.{Seeds, Validation}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.{write ⇒ render}

class FullOrderTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  implicit val formats: DefaultFormats.type = DefaultFormats

  "FullOrder" - {
    "fromOrder" in {
      db.run(Seeds.run()).futureValue
      val order = Orders.findById(1).run().futureValue.get
      val json = render(FullOrder.fromOrder(order).futureValue)
      val root = parse(json)

      (root \ "id").extract[Int] must === (1)
    }
  }
}
