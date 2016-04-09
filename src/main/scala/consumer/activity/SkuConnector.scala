package consumer.activity

import consumer.aliases.EC
import org.json4s.JsonAST.{JInt, JNothing}

import scala.concurrent.Future

final case class SkuConnector()(implicit ec: EC) extends ActivityConnector {
  val dimension = "sku"

  def process(offset: Long, activity: Activity): Future[Seq[Connection]] = Future {
    val skuIds = bySkuData(activity)
    skuIds.distinct.map(createConnection(_, activity.id))
  }

  def createConnection(formId: String, activityId: Int): Connection = {
    Connection(
      dimension  = dimension,
      objectId   = formId,
      data       = JNothing,
      activityId = activityId)
  }

  private def bySkuData(activity: Activity): Seq[String] = {
    activity.data \ "sku" \ "form" \ "id" match {
      case JInt(formId) => Seq(formId.toString)
      case _            => Seq.empty
    }
  }
}
