package services

import java.time.{Instant, ZoneId}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import models.location.Countries
import org.scalatest.mock.MockitoSugar
import util._
import util.fixtures.BakedFixtures
import utils.apis.Avalara
import utils.db._

class AvalaraTest
    extends IntegrationTestBase
    with MockitoSugar
    with MockedApis
    with TestObjectContext
    with TestActivityContext.AdminAC
    with BakedFixtures {

  import Tags._

  implicit val actorSystem: ActorSystem        = ActorSystem.create("avalara-test-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val today   = Instant.now().atZone(ZoneId.of("UTC"))
  val service = new Avalara()

  "Avalara" - {
    "validate address" - {
      "succeeds with correct address" in new Fixture {
        service.validateAddress(address, region, country)
      }
    }
  }

  trait Fixture extends CustomerAddress_Baked {
    val country = (for {
      country ← * <~ Countries.mustFindById400(region.countryId)
    } yield country).gimme
  }

}
