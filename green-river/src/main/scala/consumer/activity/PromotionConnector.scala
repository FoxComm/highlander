package consumer.activity

import scala.concurrent.Future

import consumer.aliases._

import org.json4s.JsonAST.{JInt, JString, JNothing}

object PromotionConnector extends ActivityConnector {
  val dimension = "promotion"

  def process(offset: Long, activity: Activity)(implicit ec: EC): Future[Seq[Connection]] =
    Future {
      val promotionIds = byNoteData(activity) ++: byPromotionData(activity)

      promotionIds.distinct.map(createConnection(_, activity.id))
    }

  def createConnection(formId: String, activityId: String): Connection =
    Connection(dimension = dimension, objectId = formId, data = JNothing, activityId = activityId)

  private def byPromotionData(activity: Activity): Seq[String] =
    activity.data \ "promotion" \ "id" match {
      case JInt(promotionId) ⇒ Seq(promotionId.toString)
      case _                 ⇒ Seq.empty
    }

  private def byNoteData(activity: Activity): Seq[String] =
    (activity.data \ "note" \ "referenceType", activity.data \ "entity" \ "id") match {
      case (JString(`dimension`), JInt(id)) ⇒ Seq(id.toString)
      case _                                ⇒ Seq.empty
    }
}
