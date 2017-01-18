package consumer.activity

import scala.concurrent.Future

import consumer.aliases._
import consumer.utils.JsonTransformers.extractStringSeq

import org.json4s.JsonAST.{JInt, JString, JNothing}

object AccountConnector extends ActivityConnector {
  val dimension = "account"

  def process(offset: Long, activity: Activity)(implicit ec: EC): Future[Seq[Connection]] =
    Future {
      val accountIds =
        byContextType(activity) ++:
          byNoteData(activity) ++:
            byData(activity, "account") ++:
              byData(activity, "user") ++:
                byData(activity, "admin") ++:
                  byData(activity, "customer") ++:
                    byData(activity, "assignee") ++:
                      byId(activity, "accountId") ++:
                        byId(activity, "userId") ++:
                          byId(activity, "customerId") ++:
                            byId(activity, "adminId") ++:
                              byUpdatedActivity(activity) ++:
                                byAssignmentSingleData(activity) ++:
                                  byAssignmentBulkData(activity) ++:
                                    byAssigneesData(activity)

      accountIds.distinct.map(createConnection(_, activity.id))
    }

  def createConnection(accountId: String, activityId: Int): Connection = {
    Connection(dimension = dimension,
               objectId = accountId,
               data = JNothing,
               activityId = activityId)
  }

  private def byContextType(activity: Activity): Seq[String] =
    activity.context.userType match {
      case "account"  ⇒ Seq(activity.context.userId.toString)
      case "user"     ⇒ Seq(activity.context.userId.toString)
      case "customer" ⇒ Seq(activity.context.userId.toString)
      case "admin"    ⇒ Seq(activity.context.userId.toString)
      case _          ⇒ Seq.empty
    }

  private def byNoteData(activity: Activity): Seq[String] = {
    (activity.data \ "note" \ "referenceType", activity.data \ "entity" \ "id") match {
      case (JString("account"), JInt(id))  ⇒ Seq(id.toString)
      case (JString("user"), JInt(id))     ⇒ Seq(id.toString)
      case (JString("customer"), JInt(id)) ⇒ Seq(id.toString)
      case _                               ⇒ Seq.empty
    }
  }

  private def byData(activity: Activity, fieldName: String): Seq[String] =
    activity.data \ fieldName \ "id" match {
      case JInt(value) ⇒ Seq(value.toString)
      case _           ⇒ Seq.empty
    }

  private def byId(activity: Activity, fieldName: String): Seq[String] =
    activity.data \ fieldName match {
      case JInt(accountId) ⇒ Seq(accountId.toString)
      case _               ⇒ Seq.empty
    }

  private def byUpdatedActivity(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "oldInfo" \ "id") match {
      case ("user_updated", JInt(accountId))      ⇒ Seq(accountId.toString)
      case ("customer_updated", JInt(customerId)) ⇒ Seq(customerId.toString)
      case _                                      ⇒ Seq.empty
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
      case ("bulk_assigned", JString(entity))   ⇒ extractStringSeq(activity.data, "entityIds")
      case ("bulk_unassigned", JString(entity)) ⇒ extractStringSeq(activity.data, "entityIds")
      case _                                    ⇒ Seq.empty
    }
  }

  private def byAssigneesData(activity: Activity): Seq[String] =
    extractStringSeq(activity.data, "assignees")
}
