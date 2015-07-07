import akka.http.scaladsl.model.StatusCodes

import models.{Customers, GiftCard, GiftCardCsrs, GiftCards, StoreAdmins}
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

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val (admin, customer) = (for {
      admin ← (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }
      customer ← Customers.save(Factories.customer)
    } yield (admin, customer)).run().futureValue
  }

  "returns gift cards belonging to the customer" in new Fixture {
    val gc = (for {
      origin ← GiftCardCsrs.save(Factories.giftCardCsr.copy(adminId = admin.id))
      gc ← GiftCards.save(Factories.giftCard.copy(customerId = Some(customer.id), originId = origin.id))
    } yield gc).run().futureValue

    val response = GET(s"v1/users/${customer.id}/payment-methods/gift-cards")
    val giftCard = parse(response.bodyText).extract[Seq[GiftCard]].head

    response.status must === (StatusCodes.OK)
    giftCard.customerId must === (Some(customer.id))
  }

  "returns an empty array when the customer has no gift cards" in new Fixture {
    val response = GET(s"v1/users/${customer.id}/payment-methods/gift-cards")
    val giftCards = parse(response.bodyText).extract[Seq[GiftCard]]

    response.status must === (StatusCodes.OK)
    giftCards mustBe 'empty
  }

  "finds a gift card by id" in new Fixture {
    val gc = (for {
      origin ← GiftCardCsrs.save(Factories.giftCardCsr.copy(adminId = admin.id))
      gc ← GiftCards.save(Factories.giftCard.copy(customerId = Some(customer.id), originId = origin.id))
    } yield gc).run().futureValue

    val response = GET(s"v1/users/${customer.id}/payment-methods/gift-cards/${gc.id}")
    val giftCard = parse(response.bodyText).extract[GiftCard]

    response.status must === (StatusCodes.OK)
    giftCard.customerId must === (Some(customer.id))

    val notFoundResponse = GET(s"v1/users/${customer.id}/payment-methods/gift-cards/99")
    notFoundResponse.status must === (StatusCodes.NotFound)
  }
}

