import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import models.StoreAdmins
import models.activity._
import models.customer.Customers
import org.json4s.{DefaultFormats, Extraction}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Test ⇒ QTest}
import org.scalatest.mock.MockitoSugar
import payloads.ActivityTrailPayloads.AppendActivity
import payloads.CustomerPayloads.UpdateCustomerPayload
import responses.ActivityConnectionResponse
import services.activity.CustomerTailored.CustomerUpdated
import slick.driver.PostgresDriver.api._
import util._
import utils.db._
import utils.seeds.Seeds.Factories

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
    with HttpSupport
    with AutomaticAuth
    with MockitoSugar
    with TestActivityContext.AdminAC {

  val customerActivity = "customer_activity"
  val typeName         = "customer_updated"

  "Activity Tests" - {
    "successfully creates activity after updating customer attributes" in new Fixture {

      // Update email, name, and phone number
      val payload = UpdateCustomerPayload(name = "Crazy Larry".some,
                                          email = "crazy.lary@crazy.com".some,
                                          phoneNumber = "666 666 6666".some)

      val response = PATCH(s"v1/customers/${customer.id}", payload)
      response.status must === (StatusCodes.OK)

      // Check the activity log to see if it was created
      val activity = Activities.filterByType(typeName).result.one.futureValue.value

      // Make sure the activity has all the correct information
      activity.activityType must === (typeName)

      val customerInfoChanged = activity.data.extract[CustomerUpdated]
      customerInfoChanged.newInfo.id must === (customer.id)
      customerInfoChanged.newInfo.name.value must === (payload.name.head)
      customerInfoChanged.newInfo.email.value must === (payload.email.head)
      customerInfoChanged.newInfo.phoneNumber.value must === (payload.phoneNumber.head)
    }
  }

  "PATCH /v1/trails/:dimensionName/:objectId" - {

    "successfully append one activity to new trail" in new Fixture {

      // Update email, name, and phone number. This should generate a CustomerUpdated activity
      val payload = UpdateCustomerPayload(name = "Updated Name".some,
                                          email = "updated.name@name.com".some,
                                          phoneNumber = "666 666 6666".some)

      val response = PATCH(s"v1/customers/${customer.id}", payload)
      response.status must === (StatusCodes.OK)

      // Check the activity log to see if it was created
      val activity = Activities.filterByType(typeName).result.one.futureValue.value

      // Make sure the activity has all the correct information
      activity.activityType must === (typeName)
      val customerInfoChanged = activity.data.extract[CustomerUpdated]

      // Append the activity to the trail
      val appendedConnection = appendActivity(customerActivity, customer.id, activity.id)

      appendedConnection.activityId must === (activity.id)

      // Make sure the dimension was created
      val dimension = Dimensions.findByName(customerActivity).result.one.futureValue.value

      dimension.name must === (customerActivity)

      // Make sure the trail was created
      val trail = Trails.findById(appendedConnection.trailId).result.one.futureValue.value

      // Make sure things are linked up correctly
      trail.id must === (appendedConnection.trailId)
      trail.dimensionId must === (dimension.id)
      trail.tailConnectionId must === (Some(appendedConnection.id))
    }
  }

  "append a bunch of activities to a bunch of trails a bunch of times" in {

    val dimensionList = Gen.oneOf("d1", "d2", "d3")
    val objectList    = Gen.oneOf(1, 2, 3, 4, 5, 6)
    val wordList = Gen.oneOf("hi",
                             "ho",
                             "hum",
                             "he",
                             "mo",
                             "mof",
                             "barf",
                             "poop",
                             "scoop",
                             "foop",
                             "oop",
                             "coop",
                             "doop",
                             "tarf",
                             "larg",
                             "karf",
                             "snarf",
                             "abc",
                             "cde3",
                             "mma",
                             "cca",
                             "tafda",
                             "fdafdafda",
                             "fafdafd")

    val bunchOfActivitiesAndTrails =
      forAll(dimensionList, objectList, wordList, Gen.choose(0, 1000)) {

        (dimensionName: String, objectId: Int, randomWord: String, randomNumber: Int) ⇒
          {

            //log activity
            val activity = Activities.log(DumbActivity(randomWord, randomNumber)).gimme

            //append the activity to the trail
            val appendedConnection = appendActivity(dimensionName, objectId, activity.id)

            //make sure the dimension was created
            val dimension = Dimensions.findByName(dimensionName).result.one.futureValue.value

            dimension.name must === (dimensionName)

            //make sure the trail was created
            val trail = Trails.findById(appendedConnection.trailId).result.one.futureValue.value

            //make sure the trail is created correctly if it didn't exist
            trail.id must === (appendedConnection.trailId)
            trail.dimensionId must === (dimension.id)

            //make sure connection is linked correctly
            appendedConnection.activityId must === (activity.id)
            true
          }
      }

    val qr = QTest.check(bunchOfActivitiesAndTrails) {
      _.withMaxSize(1000).withWorkers(1)
    }
    qr.passed must === (true)
  }

  def getConnection(id: Int) =
    Connections.findById(id).extract.result.head.run().futureValue

  def appendActivity(dimension: String, objectId: Int, activityId: Int) = {
    val appendPayload  = AppendActivity(activityId)
    val appendResponse = POST(s"v1/trails/$dimension/$objectId", appendPayload)

    appendResponse.status must === (StatusCodes.OK)
    appendResponse.as[ActivityConnectionResponse.Root]
  }

  trait Fixture {
    val (customer, admin) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      admin    ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (customer, admin)).gimme
  }
}
