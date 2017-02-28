package consumer.activity

import scala.concurrent.Future

import consumer.aliases._
import consumer.utils.JsonTransformers.extractBigIntSeq

import org.json4s.JsonAST.{JInt, JNothing}

object StoreCreditConnector extends ActivityConnector {
  val dimension = "store_credit"

  def process(offset: Long, activity: Activity)(implicit ec: EC): Future[Seq[Connection]] =
    Future {
      val storeCreditIds = byStoreCreditData(activity) ++: byBulkData(activity)
      storeCreditIds.distinct.map(createConnection(_, activity.id))
    }

  def createConnection(storeCreditId: String, activityId: String): Connection = {
    Connection(dimension = dimension,
               objectId = storeCreditId,
               data = JNothing,
               activityId = activityId)
  }

  private def byStoreCreditData(activity: Activity): Seq[String] = {
    activity.data \ "storeCredit" \ "id" match {
      case JInt(storeCreditId) ⇒ Seq(storeCreditId.toString)
      case _                   ⇒ Seq.empty
    }
  }

  private def byBulkData(activity: Activity): Seq[String] =
    extractBigIntSeq(activity.data, "storeCreditIds")
}
