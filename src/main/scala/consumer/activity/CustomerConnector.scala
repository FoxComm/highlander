package consumer.activity

import java.time.Instant

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.json4s.JsonAST.{JInt, JNothing}
import org.json4s.jackson.JsonMethods._

final case class CustomerConnector()(implicit ec: ExecutionContext) extends ActivityConnector {
  val dimension = "customer"
  def process(offset: Long, activity: Activity) : Future[Seq[Connection]] = Future {
    activity.data \ "customerId" match {
      case JInt(customerId) ⇒  Seq(createConnection(customerId, activity.id))
      case _ ⇒ Seq.empty
    }
  }

  def createConnection(customerId: BigInt, activityId: Int) : Connection = {
    Connection(
      dimension = dimension,
      objectId = customerId,
      data = JNothing,
      activityId = activityId)
  }
}
