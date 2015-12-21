import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import models.Customers
import models.StoreAdmins
import models.activity.Activities
import models.activity.ActivityContext
import models.activity.Trails
import models.activity.Dimensions
import models.activity.Connections
import models.activity.Connection
import models.activity.OpaqueActivity
import org.scalatest.mock.MockitoSugar
import payloads.AppendActivity
import responses.ActivityConnectionResponse
import services.NotFoundFailure404
import services.activity.CustomerUpdated
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories
import slick.driver.PostgresDriver.api._

import org.json4s.DefaultFormats
import org.json4s.Extraction

import concurrent.ExecutionContext.Implicits.global

import org.scalacheck.Prop.forAll
import org.scalacheck.{Test ⇒ QTest}
import org.scalacheck.Gen
import scala.language.implicitConversions

import Extensions._

final case class DumbActivity(
  randomWord: String, 
  randomNumber: Int)

object DumbActivity {
  val typeName = "dumb_activity"

  implicit val formats: DefaultFormats.type = DefaultFormats

  implicit def typed2opaque(a: DumbActivity) : OpaqueActivity = {
    val t = typeName
    OpaqueActivity(t, Extraction.decompose(a))
  }
}


class ActivityTrailIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with MockitoSugar {

  // paging and sorting API
  def connectionNotFound(id: Int): NotFoundFailure404 = NotFoundFailure404(Connection, id)
  val uriPrefix = "v1/trails"
  val customerActivity = "customer_activity"
  val typeName = "customer_updated"

  "Activity Tests" - {
    "successfully creates activity after updating customer attributes" in new Fixture {

      // Update email, name, and phone number
      val payload = payloads.UpdateCustomerPayload(name = "Crazy Larry".some, email = "crazy.lary@crazy.com".some,
        phoneNumber = "666 666 6666".some)

      val response = PATCH(s"v1/customers/${customer.id}", payload)
      response.status must === (StatusCodes.OK)

      // Check the activity log to see if it was created
      val activity = Activities.filterByType(typeName).result.one.futureValue.value

      // Make sure the activity has all the correct information
      activity.activityType must === (typeName)

      val customerInfoChanged = activity.data.extract[CustomerUpdated]
      customerInfoChanged.newInfo.id must                 === (customer.id)
      customerInfoChanged.newInfo.name.value must         === (payload.name.head)
      customerInfoChanged.newInfo.email.value must        === (payload.email.head)
      customerInfoChanged.newInfo.phoneNumber.value must  === (payload.phoneNumber.head)
    }
  }

  "PATCH /v1/trails/:dimensionName/:objectId" - {

    "successfully append one activity to new trail" in new Fixture {

      // Update email, name, and phone number. This should generate a CustomerUpdated activity
      val payload = payloads.UpdateCustomerPayload(
        name = "Updated Name".some, 
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
      appendedConnection.previousId must === (None)
      appendedConnection.nextId must === (None)

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

      implicit val ac = ActivityContext(userId = 1, userType = "b", transactionId = "c")

      val dimensionList = Gen.oneOf("d1", "d2", "d3")
      val objectList = Gen.oneOf(1, 2, 3, 4, 5, 6)
      val wordList = Gen.oneOf(
        "hi", "ho", "hum", "he", "mo", "mof", "barf", "poop", 
        "scoop", "foop", "oop", "coop", "doop", "tarf", "larg", "karf", "snarf",
        "abc", "cde3", "mma", "cca", "tafda", "fdafdafda", "fafdafd")

      val bunchOfActivitiesAndTrails = forAll(dimensionList, objectList, wordList, Gen.choose(0,1000)) {

        (dimensionName: String, objectId: Int, randomWord: String, randomNumber: Int) ⇒  {

            //log activity
            val activity = Activities.log(DumbActivity(randomWord, randomNumber)).run().futureValue.rightVal

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

            val newConnection = getConnection(appendedConnection.id)
            checkValidTrail(newConnection)
        }
      }

      val qr = QTest.check(bunchOfActivitiesAndTrails) { _.withMaxSize(1000).withWorkers(1)}
      qr.passed must === (true)
  }

  def getConnection(id: Int) = Connections.findById(id).extract.result.head.run().futureValue

  def appendActivity(dimension: String, objectId: Int, activityId: Int) = {
    val appendPayload = AppendActivity(activityId)
    val appendResponse = POST(s"v1/trails/$dimension/$objectId", appendPayload)

    appendResponse.status must === (StatusCodes.OK)
    appendResponse.as[ActivityConnectionResponse.Root]
  }

  def connectionLinkedCorrectly(a: Connection) : Boolean = {
    val previousLinked = a.previousId match {
      case Some(previousId) ⇒ connectionsLinked(getConnection(previousId), a)
      case None ⇒ true
    }
    val nextLinked = a.nextId match {
      case Some(nextId) ⇒ connectionsLinked(a, getConnection(nextId))
      case None ⇒ true
    }

    previousLinked && nextLinked 
  }

  def connectionsLinked(a: Connection, b: Connection) : Boolean = {
    a.nextId.contains(b.id) && b.previousId == Some(a.id)
  }

  /**
   * Walk the activity trail and validate that it is linked correctly.
   * Of course as the trail grows, validation becomes O(n^2) since we check
   * whole trail for each add.
   */
  def checkValidTrail(a:Connection, count: Int = 0) : Boolean = {
    if(count > 1000) false
    else {
      connectionLinkedCorrectly(a) && (a.previousId match { 
        case Some(previousId) ⇒ {
          val previous = getConnection(previousId)
          checkValidTrail(previous, count + 1)
        }
        case None ⇒ true
      })
    }
  }

  trait Fixture {
    val (customer, admin) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      admin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (customer, admin)).runT().futureValue.rightVal
  }
}
