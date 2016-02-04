import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes
import akka.stream.scaladsl.Source

import Extensions._
import cats.implicits._
import models.NotificationSubscription._
import models.activity._
import models.{Customers, Notification, NotificationSubscriptions, NotificationTrailMetadata, StoreAdmin, StoreAdmins}
import org.json4s.JsonAST.JString
import org.json4s.jackson.Serialization.write
import payloads.{AppendActivity, CreateNotification, UpdateCustomerPayload}
import responses.ActivityConnectionResponse.Root
import responses.LastSeenActivityResponse
import services.NotificationManager.unsubscribe
import services.{NotFoundFailure400, NotFoundFailure404, NotificationManager, NotificationTrailNotFound400}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories

class NotificationIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  import SSE._

  "SSE v1/notifications/:adminId" - {

    "streams new notifications" in new Fixture2 {
      subscribeToNotifications()
      val notifications = skipHeartbeats(sseSource(s"v1/notifications/$adminId"))
      val requests = Source(1 to 2).map { activityId ⇒
        val response = POST("v1/notifications", newNotification.copy(activityId = activityId))
        s"notification $activityId: ${response.status}"
      }

      probe(requests.interleave(notifications, segmentSize = 1))
        .requestNext("notification 1: 200 OK")
        .requestNext(activityJson(1))
        .requestNext("notification 2: 200 OK")
//        .requestNext(activityJson(2)) // FIXME
    }

    "loads old unread notifications before streaming new" in new Fixture2 {
      subscribeToNotifications()
      POST("v1/notifications", newNotification).status must === (StatusCodes.OK)
      val notifications = skipHeartbeats(sseSource(s"v1/notifications/$adminId"))

      val requests = Source.single(2).map { activityId ⇒
        val response = POST("v1/notifications", newNotification.copy(activityId = activityId))
        s"notification $activityId: ${response.status}"
      }

      probe(notifications.interleave(requests, segmentSize = 1))
        .requestNext(activityJson(1))
        .requestNext("notification 2: 200 OK")
//        .requestNext(activityJson(2)) // FIXME
    }

    "streams error and closes stream if admin not found" in {
      val message = s"Error! Store admin with id=666 not found"

      sseProbe("v1/notifications/666")
        .request(2)
        .expectNext(message)
        .expectComplete()
    }
  }

  "POST v1/notifications/:adminId/last-seen/:activityId" - {

    "updates last seen id" in new Fixture2 {
      def lastSeenId(adminId: Int) = Trails.findNotificationByAdminId(adminId).result.headOption.run().futureValue.value
        .data.value.extract[NotificationTrailMetadata].lastSeenActivityId

      subscribeToNotifications()
      POST("v1/notifications", newNotification).status must === (StatusCodes.OK)

      lastSeenId(adminId) must === (0)
      val response = POST(s"v1/notifications/$adminId/last-seen/1")
      response.status must === (StatusCodes.OK)
      val data = response.as[LastSeenActivityResponse]
      data.trailId must === (1)
      data.lastSeenActivityId must === (1)
      lastSeenId(adminId) must === (1)

      POST("v1/notifications", newNotification.copy(activityId = 2)).status must === (StatusCodes.OK)

      sseProbe(s"v1/notifications/$adminId").requestNext(activityJson(2))
    }

    "404 if admin not found" in {
      val response = POST("v1/notifications/666/last-seen/1")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 666).description)
    }

    "404 if activity not found" in {
      val adminId = StoreAdmins.create(Factories.storeAdmin).run().futureValue.rightVal.id
      val response = POST(s"v1/notifications/$adminId/last-seen/666")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Activity, 666).description)
    }

    "400 if notification trail not found" in new Fixture {
      val response = POST(s"v1/notifications/$adminId/last-seen/$activityId")
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotificationTrailNotFound400(1).description)
    }
  }

  "POST v1/notifications" - {

    "creates notification" in new Fixture {
      val response1 = POST("v1/notifications", newNotification)
      response1.status must === (StatusCodes.OK)
      response1.as[Seq[Root]] mustBe empty

      subscribeToNotifications()
      val response2 = POST("v1/notifications", newNotification)
      response2.status must === (StatusCodes.OK)
      val data = response2.as[Seq[Root]]
      data must have size 1
      val connection = data.head
      connection.activityId must === (1)
      connection.dimension must === (Dimension.notification)
    }

    "400 if source dimension not found" in {
      val response = POST("v1/notifications", newNotification)
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure400(Dimension, Dimension.order).description)
    }

    "400 if source activity not found" in {
      createDimension.run().futureValue.rightVal
      val response = POST("v1/notifications", newNotification)
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure400(Activity, 1).description)
    }
  }

  "Inner methods" - {
    "Subscribe" - {
      "successfully subscribes" in new Fixture {
        POST("v1/notifications", newNotification).status must === (StatusCodes.OK)
        Connections.result.run().futureValue mustBe empty
        subscribeToNotifications().result.value must === (1)
        val sub = NotificationSubscriptions.result.headOption.run().futureValue.value
        sub.adminId must === (1)
        sub.dimensionId must === (1)
        sub.objectId must === ("1")
        sub.reason must === (Watching)
        POST("v1/notifications", newNotification).status must === (StatusCodes.OK)
        val connections = Connections.result.run().futureValue
        connections must have size 1
        val connection = connections.headOption.value
        connection.activityId must === (1)
        connection.dimensionId must === (2)
      }

      "warns about absent admins" in new Fixture {
        val result = subscribeToNotifications(adminIds = Seq(1, 2))
        result.result.value must === (1)
        result.warnings.value must === (List(NotFoundFailure404(StoreAdmin, 2).description))
      }

      "subscribes twice for different reasons" in new Fixture {
        subscribeToNotifications(reason = Watching)
        subscribeToNotifications(reason = Assigned)
        NotificationSubscriptions.result.run().futureValue must have size 2
      }

      "ignores duplicate requests" in new Fixture {
        subscribeToNotifications()
        subscribeToNotifications()
        NotificationSubscriptions.result.run().futureValue must have size 1
      }

      "creates dimension if there is none" in {
        Dimensions.result.run().futureValue mustBe empty
        subscribeToNotifications()
        Dimensions.result.run().futureValue must have size 1
      }
    }

    "Unsubscribe" - {
      "successfully unsubscribes" in new Fixture {
        subscribeToNotifications()
        POST("v1/notifications", newNotification).status must === (StatusCodes.OK)
        Connections.result.run().futureValue must have size 1
        unsubscribeFromNotifications()
        POST("v1/notifications", newNotification).status must === (StatusCodes.OK)
        Connections.result.run().futureValue must have size 1
      }

      "distinguishes by reason" in new Fixture {
        subscribeToNotifications(reason = Watching)
        subscribeToNotifications(reason = Assigned)
        unsubscribeFromNotifications()
        val subs = NotificationSubscriptions.result.run().futureValue
        subs must have size 1
        subs.head.reason must === (Assigned)
      }

      "ignores wrong ids" in new Fixture {
        unsubscribeFromNotifications() // `rightVal` checks we got Xor.Right
      }
    }
  }

  "Notifications" - {
    val customerDimension = "customer"

    // Basic flow test for 1 admin + 1 subscription + 1 object updates
    "...must flow!" in {
      // Setup data
      (for {
        _ ← * <~ Customers.create(Factories.customer)
        _ ← * <~ StoreAdmins.create(Factories.storeAdmin)
        _ ← * <~ createDimension
      } yield {}).runTxn().futureValue

      // Let's go
      createActivityAndConnections("X")
      Activities.result.run().futureValue must have size 1

      // No notification connection/trail should be created yet, only customer ones
      connections must === (Seq((customerDimension, 1)))

      subscribeToNotifications(dimension = customerDimension)
      createActivityAndConnections("Y")
      // Both connections must be created this time
      connections must contain allOf((customerDimension, 1), (customerDimension, 2), (Dimension.notification, 2))
      // Trail must be created
      val newTrail = Trails.findNotificationByAdminId(1).result.headOption.run().futureValue.value
      newTrail.tailConnectionId.value must === (3)
      newTrail.data.value.extract[NotificationTrailMetadata].lastSeenActivityId must === (0)

      unsubscribe(adminIds = Seq(1), objectIds = Seq("1"), reason = Watching, dimension = customerDimension)
      createActivityAndConnections("Z")
      Activities.result.run().futureValue must have size 3
      // No new notification connections must appear
      connections must contain allOf((customerDimension, 1), (customerDimension, 2), (customerDimension, 3),
        (Dimension.notification, 2))
    }

    def connections = (for {
      dim ← Dimensions
      con ← Connections.filter(_.dimensionId === dim.id)
    } yield (dim.name, con.activityId)).result.run().futureValue

    def createActivityAndConnections(newName: String) = {
      // Trigger activity creation
      PATCH("v1/customers/1", UpdateCustomerPayload(name = newName.some)).status must === (StatusCodes.OK)
      // Emulate Green river calls
      val aId = Activities.sortBy(_.id.desc).result.run().futureValue.headOption.value.id
      POST("v1/trails/" + customerDimension + "/1", AppendActivity(activityId = aId, data = None)).status must === (StatusCodes.OK)
      val payload = CreateNotification(sourceDimension = customerDimension, sourceObjectId = "1", activityId = aId, data = None)
      POST("v1/notifications", payload).status must === (StatusCodes.OK)
    }
  }

  val newActivity = Activity(
    activityType = "foo",
    data = JString("data"),
    context = ActivityContext(1, "x", "y"))

  val createActivity = Activities.create(newActivity)

  val createDimension = Dimensions.create(Dimension(name = Dimension.order, description = Dimension.order))

  def activityJson(id: Int) = write(newActivity.copy(id = id))

  val newNotification = CreateNotification(sourceDimension = Dimension.order, sourceObjectId = "1", activityId = 1, data = None)

  def subscribeToNotifications(adminIds: Seq[Int] = Seq(1), dimension: String = Dimension.order,
    objectIds: Seq[String] = Seq("1"), reason: Reason = Watching) =
    NotificationManager.subscribe(adminIds = adminIds, dimension = dimension, objectIds = objectIds, reason = reason)
      .run().futureValue.rightVal

  def unsubscribeFromNotifications() =
    NotificationManager.unsubscribe(Seq(1), Seq("1"), Watching, Dimension.order).runTxn().futureValue.rightVal

  trait Fixture {
    val (adminId, activityId) = (for {
      _ ← * <~ createDimension
      admin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      activity ← * <~ createActivity
    } yield (admin.id, activity.id)).runTxn().futureValue.rightVal
  }

  trait Fixture2 {
    val adminId = (for {
      _ ← * <~ createDimension
      admin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      _ ← * <~ Activities.createAll(List.fill(2)(newActivity))
    } yield admin.id).runTxn().futureValue.rightVal
  }

}
