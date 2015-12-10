package consumer.activity

import java.time.Instant

import scala.concurrent.ExecutionContext

import org.json4s.JsonAST.JNothing
import org.json4s.jackson.JsonMethods._

final case class AdminConnector() extends ActivityConnector {
  val dimension = "admin"
  def process(offset: Long, activity: Activity)(implicit ec: ExecutionContext) : Seq[Connection] = {
    activity.context.userType match {
      case "admin" ⇒  Seq(createConnection(activity.context.userId, activity.id))
      case _ ⇒ Seq.empty
    }
  }

  def createConnection(adminId: Int, activityId: Int) : Connection = {
    Connection(
      dimension = dimension,
      objectId = adminId,
      data = JNothing,
      activityId = activityId)
  }
}
