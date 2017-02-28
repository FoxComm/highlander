package consumer.activity

import scala.concurrent.Future

import consumer.aliases._
import consumer.utils.JsonTransformers.extractStringSeq
import org.json4s.JsonAST.{JInt, JNothing}

object SharedSearchConnector extends ActivityConnector {
  val whitelist = Seq("associated_with_search", "unassociated_from_search")

  def process(offset: Long, activity: Activity)(implicit ec: EC): Future[Seq[Connection]] =
    Future {
      if (whitelist.contains(activity.activityType)) {
        val adminIds = byAssociatesData(activity) ++: byAssociateData(activity)
        adminIds.distinct.map(createConnection(_, activity.id))
      } else {
        Seq.empty
      }
    }

  def createConnection(adminId: String, activityId: String): Connection = {
    Connection(dimension = "notification",
               objectId = adminId,
               data = JNothing,
               activityId = activityId)
  }

  private def byAssociateData(activity: Activity): Seq[String] = {
    activity.data \ "associate" \ "id" match {
      case JInt(value) ⇒ Seq(value.toString)
      case _           ⇒ Seq.empty
    }
  }

  private def byAssociatesData(activity: Activity): Seq[String] =
    extractStringSeq(activity.data, "associates")
}
