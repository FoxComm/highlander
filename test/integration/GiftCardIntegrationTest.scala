import akka.http.scaladsl.model.StatusCodes

import models.{Customers, GiftCard, GiftCardManuals, GiftCards, StoreAdmins}
import org.scalatest.BeforeAndAfterEach
import util.IntegrationTestBase
import utils.Seeds.Factories

class GiftCardIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with BeforeAndAfterEach {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "admin API" - {
    "finds a gift card by id" in new Fixture {
      val gc = (for {
        origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id))
        gc ← GiftCards.save(Factories.giftCard.copy(originId = origin.id))
      } yield gc).run().futureValue

      val response = GET(s"v1/gift-cards/${gc.id}")
      val giftCard = parse(response.bodyText).extract[GiftCard]

      response.status must ===(StatusCodes.OK)
      giftCard.id must === (gc.id)

      val notFoundResponse = GET(s"v1/gift-cards/99")
      notFoundResponse.status must ===(StatusCodes.NotFound)
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val admin = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
    } yield admin).run().futureValue
  }
}

