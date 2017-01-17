package consumer.activity

import scala.concurrent.Future

import consumer.aliases._
import consumer.utils.JsonTransformers.extractStringSeq

import org.json4s.JsonAST.{JInt, JString, JNothing}

object GiftCardConnector extends ActivityConnector {
  val dimension = "gift_card"

  def process(offset: Long, activity: Activity)(implicit ec: EC): Future[Seq[Connection]] =
    Future {
      val giftCardIds =
        byGiftCardData(activity) ++: byAssignmentSingleData(activity) ++:
          byAssignmentBulkData(activity) ++: byNoteData(activity)

      giftCardIds.distinct.map(createConnection(_, activity.id))
    }

  def createConnection(code: String, activityId: Int): Connection = {
    Connection(dimension = dimension, objectId = code, data = JNothing, activityId = activityId)
  }

  private def byNoteData(activity: Activity): Seq[String] = {
    (activity.data \ "note" \ "referenceType", activity.data \ "entity" \ "code") match {
      case (JString("giftCard"), JString(code)) ⇒ Seq(code)
      case _                                    ⇒ Seq.empty
    }
  }

  private def byGiftCardData(activity: Activity): Seq[String] = {
    activity.data \ "giftCard" \ "code" match {
      case JString(refNum) ⇒ Seq(refNum)
      case _               ⇒ Seq.empty
    }
  }

  private def byAssignmentSingleData(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "entity" \ "code") match {
      case ("assigned", JString(code))   ⇒ Seq(code)
      case ("unassigned", JString(code)) ⇒ Seq(code)
      case _                             ⇒ Seq.empty
    }
  }

  private def byAssignmentBulkData(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "referenceType") match {
      case ("bulk_assigned", JString("giftCard"))   ⇒ extractStringSeq(activity.data, "entityIds")
      case ("bulk_unassigned", JString("giftCard")) ⇒ extractStringSeq(activity.data, "entityIds")
      case _                                        ⇒ Seq.empty
    }
  }
}
