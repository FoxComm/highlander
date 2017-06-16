import phoenix.failures.InvalidReasonTypeFailure
import phoenix.models.returns.ReturnReasons
import phoenix.models.{Reason, Reasons}
import phoenix.utils.seeds.Factories
import testutils._
import testutils.apis.PhoenixPublicApi
import testutils.fixtures.BakedFixtures
import core.utils.Strings._
import core.db._

class ReasonsIntegrationTest
    extends IntegrationTestBase
    with PhoenixPublicApi
    with DefaultJwtAdminAuth
    with BakedFixtures {

  "Reasons" - {

    "GET /v1/public/reasons/:type" - {
      "should return list of reasons by type" in new Fixture {
        val reasonType = Reason.GiftCardCreation.toString.lowerCaseFirstLetter

        val root = publicApi.getReason(reasonType).as[Seq[Reason]]
        root.size must === (1)
        root.headOption.value.id must === (reason.id)
      }

      "should return error if invalid type provided" in new Fixture {
        publicApi.getReason("lolwut").mustFailWith400(InvalidReasonTypeFailure("lolwut"))
      }
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val (reason, returnReason) = (for {
      reason ← * <~ Reasons.create(
                Factories.reasons.head.copy(reasonType = Reason.GiftCardCreation,
                                            storeAdminId = storeAdmin.accountId))
      returnReason ← * <~ ReturnReasons.create(Factories.returnReasons.head)
    } yield (reason, returnReason)).gimme
  }
}
