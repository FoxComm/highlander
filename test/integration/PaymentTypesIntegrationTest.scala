import Extensions._
import akka.http.scaladsl.model.StatusCodes
import models.payment.giftcard.{GiftCard, GiftCards, GiftCardManual, GiftCardManuals, GiftCardSubtypes}
import models.payment.storecredit.{StoreCredit, StoreCredits, StoreCreditManual, 
  StoreCreditManuals, StoreCreditSubtypes}
import models.customer.Customers
import models.{Reasons, StoreAdmins}
import org.scalatest.BeforeAndAfterEach
import responses.{GiftCardSubTypesResponse, StoreCreditSubTypesResponse}
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds.Factories

import scala.concurrent.ExecutionContext.Implicits.global

class PaymentTypesIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with BeforeAndAfterEach {

  "GiftCard Types" - {
    "GET /v1/public/gift-cards/types" - {
      "should return all GC types and related sub-types" in new GiftCardFixture {
        val response = GET(s"v1/public/gift-cards/types")
        response.status must ===(StatusCodes.OK)

        val root = response.as[Seq[GiftCardSubTypesResponse.Root]]
        root.size must === (GiftCard.OriginType.types.size)
        root.map(_.originType) must === (GiftCard.OriginType.types.toSeq)
        root.filter(_.originType == gcSubType.originType).head.subTypes must === (Seq(gcSubType))
      }
    }
  }

  "StoreCredits" - {
    "GET /v1/public/store-credits/types" - {
      "should return all SC types and related subtypes" in new StoreCreditFixture {
        val response = GET(s"v1/public/store-credits/types")
        response.status must ===(StatusCodes.OK)

        val root = response.as[Seq[StoreCreditSubTypesResponse.Root]]
        root.size must === (StoreCredit.OriginType.publicTypes.size)
        root.map(_.originType) must === (StoreCredit.OriginType.publicTypes.toSeq)
        root.filter(_.originType == scSubType.originType).head.subTypes must === (Seq(scSubType))
      }
    }
  }    

  trait GiftCardFixture {
    val (giftCard, gcSubType) = (for {
      admin     ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason    ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      gcSubType ← * <~ GiftCardSubtypes.create(Factories.giftCardSubTypes.head)
      origin    ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
      giftCard  ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield (giftCard, gcSubType)).runTxn().futureValue.rightVal
  }

  trait StoreCreditFixture {
    val (storeCredit, scSubType) = (for {
      admin       ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer    ← * <~ Customers.create(Factories.customer)
      scReason    ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      scSubType   ← * <~ StoreCreditSubtypes.create(Factories.storeCreditSubTypes.head)
      scOrigin    ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = admin.id, reasonId = scReason.id))
      storeCredit ← * <~ StoreCredits.create(Factories.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
    } yield (storeCredit,scSubType)).runTxn().futureValue.rightVal
  }  
}
