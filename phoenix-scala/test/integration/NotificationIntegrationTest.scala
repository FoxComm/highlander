import akka.stream.scaladsl.Source

import cats.implicits._
import failures._
import models.NotificationSubscription._
import models.account._
import models.activity._
import models.{NotificationSubscriptions, NotificationTrailMetadata}
import org.json4s.JsonAST.JString
import org.json4s.jackson.Serialization.write
import payloads.ActivityTrailPayloads.AppendActivity
import payloads.CreateNotification
import payloads.CustomerPayloads.UpdateCustomerPayload
import responses.ActivityConnectionResponse.Root
import responses.{ActivityResponse, LastSeenActivityResponse}
import services.NotificationManager
import services.NotificationManager.unsubscribe
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._

class NotificationIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  import SSE._

  "SSE v1/notifications" - {

    "streams new notifications" in new Fixture2 {
      subscribeToNotifications()
      val notifications = skipHeartbeatsAndAdminCreated(sseSource(s"v1/notifications"))
      val requests = Source(2 to 3).map { activityId ⇒
        val response = notificationsApi.create(newNotification.copy(activityId = activityId))
        s"notification $activityId: ${response.status}"
      }

      probe(requests.interleave(notifications, segmentSize = 1))
        .requestNext("notification 2: 200 OK")
        .requestNext(activityJson(2))
        .requestNext("notification 3: 200 OK")
        .requestNext(activityJson(3))
    }

    "loads old unread notifications before streaming new" in new Fixture2 {
      subscribeToNotifications()
      notificationsApi.create(newNotification).mustBeOk()
      val notifications = skipHeartbeatsAndAdminCreated(sseSource(s"v1/notifications"))

      val requests = Source.single(2).map { activityId ⇒
        val response = notificationsApi.create(newNotification.copy(activityId = activityId))
        s"notification $activityId: ${response.status}"
      }

      probe(notifications.interleave(requests, segmentSize = 1))
        .requestNext(activityJson(2))
        .requestNext("notification 2: 200 OK")
    }

    "streams error and closes stream if admin not found" in {
      val message = s"Error! User with account id=1 not found"

      sseProbe(notificationsApi.notificationsPrefix)
        .request(2)
        .expectNext(message)
        .expectComplete()
    }
  }

  "POST v1/notifications/last-seen/:activityId" - {

    "updates last seen id" in new Fixture2 {
      def lastSeenId(adminId: Int) =
        Trails
          .findNotificationByAdminId(adminId)
          .result
          .headOption
          .gimme
          .value
          .data
          .value
          .extract[NotificationTrailMetadata]
          .lastSeenActivityId

      subscribeToNotifications()
      notificationsApi.create(newNotification).mustBeOk()

      lastSeenId(adminId) must === (0)
      val data = notificationsApi.updateLastSeen(1).as[LastSeenActivityResponse]
      data.trailId must === (1)
      data.lastSeenActivityId must === (1)
      lastSeenId(adminId) must === (1)

      notificationsApi.create(newNotification.copy(activityId = 2)).mustBeOk()

      sseProbe(s"v1/notifications").requestNext(activityJson(2))
    }

    "404 if activity not found" in new StoreAdmin_Seed {
      notificationsApi.updateLastSeen(666).mustFailWith404(NotFoundFailure404(Activity, 666))
    }

    "400 if notification trail not found" in new Fixture {
      notificationsApi.updateLastSeen(activityId).mustFailWith400(NotificationTrailNotFound400(1))
    }
  }

  "POST v1/notifications" - {

    "creates notification" in new Fixture {
      notificationsApi.create(newNotification).as[Seq[Root]] mustBe empty

      subscribeToNotifications()

      val connection = notificationsApi.create(newNotification).as[Seq[Root]].onlyElement
      connection.activityId must === (1)
      connection.dimension must === (Dimension.notification)
    }

    "400 if source dimension not found" in {
      notificationsApi
        .create(newNotification)
        .mustFailWith400(NotFoundFailure400(Dimension, Dimension.order))
    }

    "400 if source activity not found" in {
      createDimension.gimme
      notificationsApi.create(newNotification).mustFailWith400(NotFoundFailure400(Activity, 1))
    }
  }

  "Inner methods" - {
    "Subscribe" - {
      "successfully subscribes" in new Fixture {
        notificationsApi.create(newNotification).mustBeOk()
        Connections.gimme mustBe empty
        subscribeToNotifications().result.value must === (1)
        val sub = NotificationSubscriptions.one.gimme.value
        sub.adminId must === (1)
        sub.dimensionId must === (1)
        sub.objectId must === ("1")
        sub.reason must === (Watching)
        notificationsApi.create(newNotification).mustBeOk()

        val connection = Connections.gimme.onlyElement
        connection.activityId must === (1)
        connection.dimensionId must === (2)
      }

      "warns about absent admins" in new Fixture {
        val result = subscribeToNotifications(adminIds = Seq(1, 2))
        result.result.value must === (1)
        result.warnings.value must === (List(NotFoundFailure404(User, 2).description))
      }

      "subscribes twice for different reasons" in new Fixture {
        subscribeToNotifications(reason = Watching)
        subscribeToNotifications(reason = Assigned)
        NotificationSubscriptions.gimme must have size 2
      }

      "ignores duplicate requests" in new Fixture {
        subscribeToNotifications()
        subscribeToNotifications()
        NotificationSubscriptions.gimme must have size 1
      }

      "creates dimension if there is none" in {
        Dimensions.gimme mustBe empty
        subscribeToNotifications()
        Dimensions.gimme must have size 1
      }
    }

    "Unsubscribe" - {
      "successfully unsubscribes" in new Fixture {
        subscribeToNotifications()
        notificationsApi.create(newNotification).mustBeOk()
        Connections.gimme must have size 1
        unsubscribeFromNotifications()
        notificationsApi.create(newNotification).mustBeOk()
        Connections.gimme must have size 1
      }

      "distinguishes by reason" in new Fixture {
        subscribeToNotifications(reason = Watching)
        subscribeToNotifications(reason = Assigned)
        unsubscribeFromNotifications()
        NotificationSubscriptions.gimme.onlyElement.reason must === (Assigned)
      }

      "ignores wrong ids" in new Fixture {
        unsubscribeFromNotifications() // `rightVal` checks we got Xor.Right
      }
    }
  }

  "Notifications" - {
    val customerDimension = "customer"

    // Basic flow test for 1 admin + 1 subscription + 1 object updates
    "...must flow!" in new Customer_Seed with StoreAdmin_Seed {
      // Setup data
      createDimension.gimme

      // Let's go
      createActivityAndConnections("X")
      Activities.gimme must have size 3 //includes customer and admin creation activity

      // No notification connection/trail should be created yet, only customer ones
      connections must === (Seq((customerDimension, 3)))

      subscribeToNotifications(dimension = customerDimension)
      createActivityAndConnections("Y")
      // Both connections must be created this time
      connections must contain allOf ((customerDimension, 3), (customerDimension, 4), (Dimension.notification,
                                                                                       4))
      // Trail must be created
      val newTrail = Trails.findNotificationByAdminId(1).one.gimme.value
      newTrail.tailConnectionId.value must === (3)
      newTrail.data.value.extract[NotificationTrailMetadata].lastSeenActivityId must === (0)

      unsubscribe(adminIds = Seq(1),
                  objectIds = Seq("1"),
                  reason = Watching,
                  dimension = customerDimension)
      createActivityAndConnections("Z")
      Activities.gimme must have size 5
      // No new notification connections must appear
      connections must contain allOf ((customerDimension, 3), (customerDimension, 4),
          (Dimension.notification, 4), (customerDimension, 5), (Dimension.notification, 5))
    }

    def connections =
      (for {
        dim ← Dimensions
        con ← Connections.filter(_.dimensionId === dim.id)
      } yield (dim.name, con.activityId)).gimme

    def createActivityAndConnections(newName: String) = {
      // Trigger activity creation
      customersApi(1).update(UpdateCustomerPayload(name = newName.some)).mustBeOk()
      // Emulate Green river calls
      val aId = Activities.sortBy(_.id.desc).gimme.headOption.value.id
      activityTrailsApi
        .appendActivity(customerDimension, 1, AppendActivity(activityId = aId, data = None))
        .mustBeOk()
      val payload = CreateNotification(sourceDimension = customerDimension,
                                       sourceObjectId = "1",
                                       activityId = aId,
                                       data = None)
      notificationsApi.create(payload).mustBeOk()
    }
  }

  val newActivity =
    Activity(activityType = "foo", data = JString("data"), context = ActivityContext(1, "x", "y"))

  val createActivity = Activities.create(newActivity)

  val createDimension =
    Dimensions.create(Dimension(name = Dimension.order, description = Dimension.order))

  def activityJson(id: Int) =
    write(ActivityResponse.build(newActivity.copy(id = id)))

  val newNotification = CreateNotification(sourceDimension = Dimension.order,
                                           sourceObjectId = "1",
                                           activityId = 1,
                                           data = None)

  def subscribeToNotifications(adminIds: Seq[Int] = Seq(1),
                               dimension: String = Dimension.order,
                               objectIds: Seq[String] = Seq("1"),
                               reason: Reason = Watching) =
    NotificationManager
      .subscribe(adminIds = adminIds,
                 dimension = dimension,
                 objectIds = objectIds,
                 reason = reason)
      .gimme

  def unsubscribeFromNotifications() =
    NotificationManager.unsubscribe(Seq(1), Seq("1"), Watching, Dimension.order).gimme

  trait Fixture extends StoreAdmin_Seed {
    val (adminId, activityId) = (for {
      _        ← * <~ createDimension
      activity ← * <~ createActivity
    } yield (storeAdmin.accountId, activity.id)).gimme
  }

  trait Fixture2 extends StoreAdmin_Seed {
    val adminId = (for {
      _ ← * <~ createDimension
      _ ← * <~ Activities.createAll(List.fill(2)(newActivity))
    } yield storeAdmin.accountId).gimme
  }
}
