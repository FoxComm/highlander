package utils

import com.typesafe.scalalogging.Logger
import core.utils._
import java.util.concurrent.{Executors, ScheduledExecutorService}
import org.scalatest.mockito.MockitoSugar
import phoenix.payloads.GiftCardPayloads.GiftCardCreateByCsr
import phoenix.payloads.NotePayloads.CreateNote
import phoenix.utils._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import testutils.TestBase

class UtilsTest extends TestBase {
  implicit val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

  "camelToUnderscores" - {
    "should convert camelCase string to snake_case string" in {
      camelToUnderscores("order") must === ("order")
      camelToUnderscores("orderShipping") must === ("order_shipping")
      camelToUnderscores("orderShippingAddress") must === ("order_shipping_address")
    }
  }

  "camelToUnderscores" - {
    "should convert camelCase class to snake_case string" in {
      snakeCaseName(CreateNote(body = "test")) must === ("create_note")
      snakeCaseName(GiftCardCreateByCsr(balance = 10, reasonId = 1)) must === ("gift_card_create_by_csr")
    }
  }

  "timeoutAfter" - {
    "should allow to complete future before timeout exceed" in {
      val f = Future(42).timeoutAfter(50.millis, null)(666)
      f.futureValue must === (42)
    }

    "should timeout with specified result after given time" in {
      val f = Future {
        Thread.sleep(100)
        42
      }.timeoutAfter(50.millis, null)(666)
      f.futureValue must === (666)
    }
  }
}
