package consumer.activity

import scala.concurrent.Future

import consumer.aliases._
import consumer.utils.JsonTransformers.extractStringSeq

import org.json4s.JsonAST.{JString, JNothing}

final case class OrderConnector()(implicit ec: EC) extends ActivityConnector {
  val dimension = "order"

  def process(offset: Long, activity: Activity): Future[Seq[Connection]] = Future {
    val orderIds = byOrderData(activity) ++: byAssignmentBulkData(activity) ++:
      byAssignmentSingleData(activity) ++: byBulkData(activity)

    orderIds.distinct.map(createConnection(_, activity.id))
  }

  def createConnection(refNum: String, activityId: Int): Connection = {
    Connection(
      dimension = dimension,
      objectId = refNum,
      data = JNothing,
      activityId = activityId)
  }

  private def byOrderData(activity: Activity): Seq[String] = {
    activity.data \ "order" \ "referenceNumber" match {
      case JString(refNum) ⇒ Seq(refNum)
      case _               ⇒ Seq.empty
    }
  }

  private def byAssignmentSingleData(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "entity" \ "referenceNumber") match {
      case ("assigned", JString(refNum))   ⇒ Seq(refNum)
      case ("unassigned", JString(refNum)) ⇒ Seq(refNum)
      case _                               ⇒ Seq.empty
    }
  }

  private def byBulkData(activity: Activity): Seq[String] = extractStringSeq(activity.data, "orderRefNums")

  private def byAssignmentBulkData(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "referenceType") match {
      case ("bulk_assigned", JString("order"))   ⇒ extractStringSeq(activity.data, "entityIds")
      case ("bulk_unassigned", JString("order")) ⇒ extractStringSeq(activity.data, "entityIds")
      case _                                     ⇒ Seq.empty
    }
  }
}
