package consumer.activity

import scala.concurrent.Future

import consumer.aliases._
import consumer.utils.JsonTransformers.extractStringSeq
import org.json4s.JsonAST.{JInt, JNothing}

final case class AdminConnector()(implicit ec: EC) extends ActivityConnector {
  val dimension = "admin"

  def process(offset: Long, activity: Activity): Future[Seq[Connection]] = Future {
    // TODO: Reduce un-necessary extractions by matching activity name
    val adminIds = byContextUserType(activity) ++: byAdminData(activity, "admin") ++:
      byAssigneesData(activity) ++: byAdminData(activity, "assignee") ++:
      byWatchersData(activity) ++: byAdminData(activity, "watcher")

    adminIds.distinct.map(createConnection(_, activity.id))
  }

  def createConnection(adminId: String, activityId: Int): Connection = {
    Connection(
      dimension = dimension,
      objectId = adminId,
      data = JNothing,
      activityId = activityId)
  }

  private def byContextUserType(activity: Activity): Seq[String] = {
    activity.context.userType match {
      case "admin" ⇒ Seq(activity.context.userId.toString)
      case _       ⇒ Seq.empty
    }
  }

  private def byAdminData(activity: Activity, fieldName: String): Seq[String] = {
    activity.data \ fieldName \ "id" match {
      case JInt(value) ⇒ Seq(value.toString)
      case _           ⇒ Seq.empty
    }
  }

  private def byAssigneesData(activity: Activity): Seq[String] = extractStringSeq(activity.data, "assignees")
  private def byWatchersData(activity: Activity): Seq[String] = extractStringSeq(activity.data, "watchers")
}
