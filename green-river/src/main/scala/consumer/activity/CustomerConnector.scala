package consumer.activity

import scala.concurrent.Future

import consumer.aliases._
import consumer.utils.JsonTransformers.extractStringSeq

import org.json4s.JsonAST.{JInt, JString, JNothing}

final case class CustomerConnector()(implicit ec: EC) extends ActivityConnector {
  val dimension = "customer"

  def process(offset: Long, activity: Activity): Future[Seq[Connection]] = Future {
    val customerIds =
      byContextUserType(activity) ++: byCustomerData(activity) ++:
      byCustomerUpdatedActivity(activity) ++: byAssignmentBulkData(activity) ++:
      byAssignmentSingleData(activity) ++: byNoteData(activity)

    customerIds.distinct.map(createConnection(_, activity.id))
  }

  def createConnection(customerId: String, activityId: Int): Connection = {
    Connection(dimension = dimension,
               objectId = customerId,
               data = JNothing,
               activityId = activityId)
  }

  private def byContextUserType(activity: Activity): Seq[String] =
    activity.context.userType match {
      case "customer" ⇒ Seq(activity.context.userId.toString)
      case _          ⇒ Seq.empty
    }

  private def byNoteData(activity: Activity): Seq[String] = {
    (activity.data \ "note" \ "referenceType", activity.data \ "entity" \ "id") match {
      case (JString("customer"), JInt(id)) ⇒ Seq(id.toString)
      case _                               ⇒ Seq.empty
    }
  }

  private def byCustomerData(activity: Activity): Seq[String] =
    activity.data \ "customer" \ "id" match {
      case JInt(customerId) ⇒ Seq(customerId.toString)
      case _                ⇒ Seq.empty
    }

  private def byCustomerUpdatedActivity(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "oldInfo" \ "id") match {
      case ("customer_updated", JInt(customerId)) ⇒ Seq(customerId.toString)
      case _                                      ⇒ Seq.empty
    }
  }

  private def byAssignmentSingleData(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "entity" \ "id") match {
      case ("assigned", JInt(customerId))   ⇒ Seq(customerId.toString)
      case ("unassigned", JInt(customerId)) ⇒ Seq(customerId.toString)
      case _                                ⇒ Seq.empty
    }
  }

  private def byAssignmentBulkData(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "referenceType") match {
      case ("bulk_assigned", JString("customer"))   ⇒ extractStringSeq(activity.data, "entityIds")
      case ("bulk_unassigned", JString("customer")) ⇒ extractStringSeq(activity.data, "entityIds")
      case _                                        ⇒ Seq.empty
    }
  }
}
