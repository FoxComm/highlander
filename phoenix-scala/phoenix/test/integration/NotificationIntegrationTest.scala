import java.time.Instant

import akka.stream.scaladsl.Source
import com.github.tminglei.slickpg.LTree
import core.failures._
import org.json4s.Extraction
import org.json4s.JsonAST._
import org.json4s.jackson.Serialization.write
import phoenix.models.NotificationSubscription._
import phoenix.models.account._
import phoenix.models.activity._
import phoenix.models.{Notification, NotificationSubscriptions, Notifications}
import phoenix.payloads.{CreateNotification, NotificationActivity}
import phoenix.responses.{ActivityResponse, LastSeenNotificationResponse, NotificationResponse}
import phoenix.services.NotificationManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import core.db._

class NotificationIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with BakedFixtures {

  import SSE._

  "SSE v1/notifications" - {

    "streams new notifications" in new Fixture {
      subscribeToNotifications()
      val notifications =
        skipHeartbeatsAndAdminCreated(sseSource(s"v1/notifications", defaultAdminAuth.jwtCookie))
      val requests = Source(2 to 3).map { activityId ⇒
        val response = notificationsApi.create(
          newNotificationPayload.copy(
            activity = newNotificationPayload.activity.copy(id = activityId.toString)))
        s"notification $activityId: ${response.status}"
      }
    }

    "loads old unread notifications before streaming new" in new Fixture {
      subscribeToNotifications()
      notificationsApi.create(newNotificationPayload).mustBeOk()
      val notifications =
        skipHeartbeatsAndAdminCreated(sseSource(s"v1/notifications", defaultAdminAuth.jwtCookie))

      val requests = Source.single(2).map { activityId ⇒
        val response = notificationsApi.create(newNotificationPayload.copy(
          activity = newNotificationPayload.activity.copy(id = activityId.toString)))
        s"notification $activityId: ${response.status}"
      }
    }

  }

  "POST v1/notifications/last-seen/:activityId" - {

    "updates last seen id" in new Fixture {
      subscribeToNotifications()
      notificationsApi.create(newNotificationPayload).mustBeOk()

      // FIXME: how to get this ID w/o peaking into the DB? @michalrus
      val notificationId = {
        import slick.jdbc.PostgresProfile.api._
        Notifications.result.gimme.onlyElement.id
      }

      val data = notificationsApi.updateLastSeen(notificationId).as[LastSeenNotificationResponse]
      data.lastSeenNotificationId must === (notificationId)

      notificationsApi
        .create(newNotificationPayload.copy(activity = newNotificationPayload.activity.copy(id = "test2")))
        .mustBeOk()
    }
  }

  "POST v1/notifications" - {

    "creates notification" in new Fixture {
      subscribeToNotifications()

      val activity = notificationsApi.create(newNotificationPayload).as[ActivityResponse]
      activity.id must === ("test")
    }
  }

  "Inner methods" - {
    "Subscribe" - {
      "successfully subscribes" in new Fixture {
        notificationsApi.create(newNotificationPayload).mustBeOk()

        subscribeToNotifications().result.value must === (1)
        val sub = NotificationSubscriptions.one.gimme.value
        sub.adminId must === (defaultAdmin.id)
        sub.dimensionId must === (dimension.id)
        sub.objectId must === (randomObjectId)
        sub.reason must === (Watching)
        val activity = notificationsApi.create(newNotificationPayload).as[ActivityResponse]
        activity.id must === ("test")
      }

      "warns about absent admins" in new Fixture {
        val result = subscribeToNotifications(adminIds = Seq(defaultAdmin.id, 2))
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
        unsubscribeFromNotifications() // `rightVal` checks we got Either.right
      }
    }
  }

  val newNotificationActivity = NotificationActivity(
    id = "test",
    kind = "test",
    data = JNothing,
    context = ActivityContext(userId = 1, userType = "x", transactionId = "y", scope = LTree("1")),
    createdAt = Instant.now)

  val newNotificationPayload = CreateNotification(sourceDimension = Dimension.order,
                                                  sourceObjectId = randomObjectId,
                                                  activity = newNotificationActivity)

  lazy val randomObjectId = scala.util.Random.alphanumeric.take(7).mkString

  def subscribeToNotifications(adminIds: Seq[Int] = Seq(defaultAdmin.id),
                               dimension: String = Dimension.order,
                               objectIds: Seq[String] = Seq(randomObjectId),
                               reason: Reason = Watching) =
    NotificationManager
      .subscribe(adminIds = adminIds, dimension = dimension, objectIds = objectIds, reason = reason)
      .gimme

  def unsubscribeFromNotifications() =
    NotificationManager
      .unsubscribe(adminIds = Seq(defaultAdmin.id), Seq(randomObjectId), Watching, Dimension.order)
      .gimme

  trait Fixture extends StoreAdmin_Seed {
    val dimension =
      Dimensions.create(Dimension(name = Dimension.order, description = Dimension.order)).gimme
    val adminId = storeAdmin.accountId
  }
}
