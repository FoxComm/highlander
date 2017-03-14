import akka.stream.scaladsl.Source

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import failures._
import models.NotificationSubscription._
import models.Notification
import models.account._
import models.activity._
import models.{NotificationSubscriptions}
import org.json4s.Extraction
import org.json4s.JsonAST._
import org.json4s.jackson.Serialization.write
import payloads.{CreateNotification, NotificationActivity}
import responses.{NotificationResponse, ActivityResponse, LastSeenNotificationResponse}
import services.NotificationManager
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
        val response = notificationsApi.create(newNotificationPayload.copy(
                activity = newNotificationPayload.activity.copy(id = activityId.toString)))
        s"notification $activityId: ${response.status}"
      }
    }

    "loads old unread notifications before streaming new" in new Fixture2 {
      subscribeToNotifications()
      notificationsApi.create(newNotificationPayload).mustBeOk()
      val notifications = skipHeartbeatsAndAdminCreated(sseSource(s"v1/notifications"))

      val requests = Source.single(2).map { activityId ⇒
        val response = notificationsApi.create(newNotificationPayload.copy(
                activity = newNotificationPayload.activity.copy(id = activityId.toString)))
        s"notification $activityId: ${response.status}"
      }
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

      subscribeToNotifications()
      notificationsApi.create(newNotificationPayload).mustBeOk()

      val data = notificationsApi.updateLastSeen(1).as[LastSeenNotificationResponse]
      data.lastSeenNotificationId must === (1)

      notificationsApi
        .create(newNotificationPayload.copy(
                activity = newNotificationPayload.activity.copy(id = "test2")))
        .mustBeOk()
    }
  }

  "POST v1/notifications" - {

    "creates notification" in new Fixture {
      subscribeToNotifications()

      val activity = notificationsApi.create(newNotificationPayload).as[ActivityResponse.Root]
      activity.id must === ("test")
    }
  }

  "Inner methods" - {
    "Subscribe" - {
      "successfully subscribes" in new Fixture {
        notificationsApi.create(newNotificationPayload).mustBeOk()
        subscribeToNotifications().result.value must === (1)
        val sub = NotificationSubscriptions.one.gimme.value
        sub.adminId must === (1)
        sub.dimensionId must === (1)
        sub.objectId must === ("1")
        sub.reason must === (Watching)
        val activity = notificationsApi.create(newNotificationPayload).as[ActivityResponse.Root]
        activity.id must === ("test")
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
        notificationsApi.create(newNotificationPayload).mustBeOk()
        unsubscribeFromNotifications()
        notificationsApi.create(newNotificationPayload).mustBeOk()
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

  val createDimension =
    Dimensions.create(Dimension(name = Dimension.order, description = Dimension.order))

  val newNotificationActivity = NotificationActivity(
      id = "test",
      kind = "test",
      data = JNothing,
      context =
        ActivityContext(userId = 1, userType = "x", transactionId = "y", scope = LTree("1")),
      createdAt = Instant.now)

  val newNotificationPayload = CreateNotification(sourceDimension = Dimension.order,
                                                  sourceObjectId = "1",
                                                  activity = newNotificationActivity)

  def activityJson(id: String) = {
    val newNotification = Notification(
        id = 2,
        scope = LTree("1"),
        accountId = 1,
        dimensionId = 1,
        objectId = "1",
        activity = Extraction.decompose(newNotificationActivity.copy(id = id)),
        createdAt = Instant.now)

    write(NotificationResponse.build(newNotification))
  }

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
    createDimension.gimme
    val adminId = storeAdmin.accountId
  }

  trait Fixture2 extends StoreAdmin_Seed {
    createDimension.gimme
    val adminId = storeAdmin.accountId
  }
}
