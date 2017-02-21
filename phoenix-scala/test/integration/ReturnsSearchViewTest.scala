import java.time.Instant

import cats.implicits._
import models.objects.ObjectContexts
import payloads.ImagePayloads.{AlbumPayload, ImagePayload}
import testutils._
import utils.Money.Currency
import utils.aliases.Json
import utils.db.ExPostgresDriver.api._

case class ReturnsSearchViewResult(
    // TBD
)

class ReturnsSearchViewTest extends SearchViewTestBase {

  type SearchViewResult = ReturnsSearchViewResult
  val searchViewName: String = "returns_search_view"
  val searchKeyName: String  = "variant_id"

}

object ReturnsSearchViewTest {}
