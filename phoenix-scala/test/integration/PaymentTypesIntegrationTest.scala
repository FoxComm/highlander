import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import models.Reasons
import models.payment.giftcard._
import models.payment.storecredit._
import responses.{GiftCardSubTypesResponse, StoreCreditSubTypesResponse}
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class PaymentTypesIntegrationTest
    extends IntegrationTestBase
    with PhoenixPublicApi
    with BakedFixtures {

  "GiftCard Types" - {
    "GET /v1/public/gift-cards/types" - {
      "should return all GC types and related sub-types" in new GiftCardFixture {
        val response = publicApi.giftCardTypes()
        response.status must === (StatusCodes.OK)

        val root = response.as[Seq[GiftCardSubTypesResponse.Root]]
        root.size must === (GiftCard.OriginType.types.size)
        root.map(_.originType) must === (GiftCard.OriginType.types.toSeq)
        root.filter(_.originType == giftCardSubtype.originType).head.subTypes must === (
            Seq(giftCardSubtype))
      }
    }
  }

  "StoreCredits" - {
    "GET /v1/public/store-credits/types" - {
      "should return all SC types and related subtypes" in new StoreCreditFixture {
        val response = publicApi.storeCreditTypes()
        response.status must === (StatusCodes.OK)

        val root = response.as[Seq[StoreCreditSubTypesResponse.Root]]
        root.size must === (StoreCredit.OriginType.publicTypes.size)
        root.map(_.originType) must === (StoreCredit.OriginType.publicTypes.toSeq)
        root.filter(_.originType == scSubType.originType).head.subTypes must === (Seq(scSubType))
      }
    }
  }

  trait GiftCardFixture extends StoreAdmin_Seed with GiftCardSubtype_Seed {
    val (giftCard) = (for {
      reason ← * <~ Reasons.create(Factories.reason(storeAdmin.id))
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).gimme
  }

  trait StoreCreditFixture extends Customer_Seed with StoreAdmin_Seed {
    val (storeCredit, scSubType) = (for {
      scReason  ← * <~ Reasons.create(Factories.reason(storeAdmin.id))
      scSubType ← * <~ StoreCreditSubtypes.create(Factories.storeCreditSubTypes.head)
      scOrigin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = storeAdmin.id, reasonId = scReason.id))
      storeCredit ← * <~ StoreCredits.create(
                       Factories.storeCredit.copy(originId = scOrigin.id,
                                                  customerId = customer.id))
    } yield (storeCredit, scSubType)).gimme
  }
}
