package consumer.activity

import consumer.aliases.EC
import org.json4s.JsonAST.{JInt, JNothing, JString}
import scala.concurrent.Future

object SkuConnector extends ActivityConnector {
  val dimension = "sku"

  def process(offset: Long, activity: Activity)(implicit ec: EC): Future[Seq[Connection]] =
    Future {
      val skuIds = bySkuData(activity) ++: byNoteData(activity)
      skuIds.distinct.map(createConnection(_, activity.id))
    }

  def createConnection(formId: String, activityId: Int): Connection = {
    Connection(dimension = dimension, objectId = formId, data = JNothing, activityId = activityId)
  }

  private def bySkuData(activity: Activity): Seq[String] = {
    activity.data \ "sku" \ "id" match {
      case JInt(id) ⇒ Seq(id.toString)
      case _        ⇒ Seq.empty
    }
  }

  private def byNoteData(activity: Activity): Seq[String] = {
    (activity.data \ "note" \ "referenceType", activity.data \ "entity" \ "id") match {
      case (JString("sku"), JInt(id)) ⇒ Seq(id.toString)
      case _                          ⇒ Seq.empty
    }
  }
}
