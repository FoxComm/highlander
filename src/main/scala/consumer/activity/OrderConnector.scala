package consumer.activity

import scala.concurrent.Future

import consumer.aliases._
import consumer.utils.JsonTransformers.extractStringSeq

import org.json4s.JsonAST.{JString, JNothing}

final case class OrderConnector()(implicit ec: EC) extends ActivityConnector {
  val dimension = "order"

  def process(offset: Long, activity: Activity): Future[Seq[Connection]] = Future {
    val orderIds = byOrderData(activity) ++: byBulkData(activity)
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

  private def byBulkData(activity: Activity): Seq[String] = extractStringSeq(activity.data, "orderRefNums")
}
