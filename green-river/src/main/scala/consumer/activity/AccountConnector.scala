package consumer.activity

import scala.concurrent.Future

import consumer.aliases._
import consumer.utils.JsonTransformers.extractStringSeq

import org.json4s.JsonAST.{JInt, JString, JNothing}

final case class AccountConnector()(implicit ec: EC) extends ActivityConnector {
  val dimension = "account"

  def process(offset: Long, activity: Activity): Future[Seq[Connection]] = Future {
    val accountIds =
      byContextUserType(activity) ++: byContextAccountType(activity) ++: byAccountData(activity) ++:
      byAccountUpdatedActivity(activity) ++: byAssignmentBulkData(activity) ++:
      byAssignmentSingleData(activity) ++: byNoteData(activity) ++: byAccountId(activity) ++:
      byUserData(activity) ++: byUserId(activity)

    accountIds.distinct.map(createConnection(_, activity.id))
  }

  def createConnection(accountId: String, activityId: Int): Connection = {
    Connection(dimension = dimension,
               objectId = accountId,
               data = JNothing,
               activityId = activityId)
  }

  private def byContextAccountType(activity: Activity): Seq[String] =
    activity.context.userType match {
      case "account" ⇒ Seq(activity.context.userId.toString)
      case _         ⇒ Seq.empty
    }

  private def byContextUserType(activity: Activity): Seq[String] =
    activity.context.userType match {
      case "user" ⇒ Seq(activity.context.userId.toString)
      case _      ⇒ Seq.empty
    }

  private def byNoteData(activity: Activity): Seq[String] = {
    (activity.data \ "note" \ "referenceType", activity.data \ "entity" \ "id") match {
      case (JString("user"), JInt(id)) ⇒ Seq(id.toString)
      case _                           ⇒ Seq.empty
    }
  }

  private def byAccountData(activity: Activity): Seq[String] =
    activity.data \ "account" \ "id" match {
      case JInt(accountId) ⇒ Seq(accountId.toString)
      case _               ⇒ Seq.empty
    }

  private def byUserData(activity: Activity): Seq[String] =
    activity.data \ "user" \ "id" match {
      case JInt(accountId) ⇒ Seq(accountId.toString)
      case _               ⇒ Seq.empty
    }

  private def byUserId(activity: Activity): Seq[String] =
    activity.data \ "userId" match {
      case JInt(accountId) ⇒ Seq(accountId.toString)
      case _               ⇒ Seq.empty
    }

  private def byAccountId(activity: Activity): Seq[String] =
    activity.data \ "accountId" match {
      case JInt(accountId) ⇒ Seq(accountId.toString)
      case _               ⇒ Seq.empty
    }

  private def byAccountUpdatedActivity(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "oldInfo" \ "id") match {
      case ("user_updated", JInt(accountId)) ⇒ Seq(accountId.toString)
      case _                                 ⇒ Seq.empty
    }
  }

  private def byAssignmentSingleData(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "entity" \ "id") match {
      case ("assigned", JInt(accountId))   ⇒ Seq(accountId.toString)
      case ("unassigned", JInt(accountId)) ⇒ Seq(accountId.toString)
      case _                               ⇒ Seq.empty
    }
  }

  private def byAssignmentBulkData(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "referenceType") match {
      case ("bulk_assigned", JString("user"))   ⇒ extractStringSeq(activity.data, "entityIds")
      case ("bulk_unassigned", JString("user")) ⇒ extractStringSeq(activity.data, "entityIds")
      case _                                    ⇒ Seq.empty
    }
  }
}
