import scala.language.implicitConversions

import cats.implicits._
import models.activity._
import org.json4s.{DefaultFormats, Extraction}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Test ⇒ QTest}
import org.scalatest.mockito.MockitoSugar
import payloads.ActivityTrailPayloads.AppendActivity
import payloads.CustomerPayloads.UpdateCustomerPayload
import responses.ActivityConnectionResponse
import responses.ActivityConnectionResponse.Root
import services.activity.CustomerTailored.CustomerUpdated
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

case class DumbActivity(randomWord: String, randomNumber: Int)

object DumbActivity {
  val typeName = "dumb_activity"

  implicit val formats: DefaultFormats.type = DefaultFormats

  implicit def typed2opaque(a: DumbActivity): OpaqueActivity = {
    val t = typeName
    OpaqueActivity(t, Extraction.decompose(a))
  }
}

class ActivityTrailIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockitoSugar
    with TestActivityContext.AdminAC
    with BakedFixtures {

  val customerActivity = "customer_activity"
  val typeName         = "customer_updated"

  def getConnection(id: Int): Connection =
    Connections.findById(id).extract.result.head.gimme

  def appendActivity(dimension: String, objectId: Int, activityId: Int): Root =
    activityTrailsApi
      .appendActivity(dimension, objectId, AppendActivity(activityId))
      .as[ActivityConnectionResponse.Root]

  trait Fixture extends Customer_Seed with StoreAdmin_Seed
}
