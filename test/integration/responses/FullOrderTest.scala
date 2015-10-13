package responses

import models.Orders
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.{write ⇒ render}
import util.IntegrationTestBase
import utils.Seeds
import utils.Slick.implicits._

class FullOrderTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  implicit val formats: DefaultFormats.type = DefaultFormats

  "FullOrder" - {
    "fromOrder" in {
      db.run(Seeds.run()).futureValue
      val order = Orders.findOneById(1).run().futureValue.value
      val json = render(FullOrder.fromOrder(order).run().futureValue)
      val root = parse(json)

      (root \ "id").extract[Int] must === (1)
    }
  }
}
