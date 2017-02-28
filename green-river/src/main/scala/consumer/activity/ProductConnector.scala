package consumer.activity

import scala.concurrent.Future

import consumer.aliases._

import org.json4s.JsonAST.{JInt, JString, JNothing}

object ProductConnector extends ActivityConnector {
  val dimension = "product"

  def process(offset: Long, activity: Activity)(implicit ec: EC): Future[Seq[Connection]] =
    Future {
      val productIds = byProductData(activity)

      productIds.distinct.map(createConnection(_, activity.id))
    }

  def createConnection(formId: String, activityId: String): Connection = {
    Connection(dimension = dimension,
               objectId = formId,
               data = JNothing,
               activityId = activityId)
  }

  private def byProductData(activity: Activity): Seq[String] = {
    activity.data \ "product" \ "id" match {
      case JInt(formId) ⇒ Seq(formId.toString)
      case _            ⇒ Seq.empty
    }
  }
}
