package consumer.activity

import scala.concurrent.Future

import consumer.aliases._

import org.json4s.JsonAST.{JInt, JString, JNothing}

final case class PromotionConnector()(implicit ec: EC) extends ActivityConnector {
  val dimension = "promotion"

  def process(offset: Long, activity: Activity): Future[Seq[Connection]] = Future {
    val promoIds = byNoteData(activity)

    promoIds.distinct.map(createConnection(_, activity.id))
  }

  def createConnection(formId: String, activityId: Int): Connection = {
    Connection(dimension = dimension, objectId = formId, data = JNothing, activityId = activityId)
  }

  private def byNoteData(activity: Activity): Seq[String] = {
    (activity.data \ "note" \ "referenceType", activity.data \ "entity" \ "id") match {
      case (JString(`dimension`), JInt(id)) ⇒ Seq(id.toString)
      case _                                ⇒ Seq.empty
    }
  }
}
