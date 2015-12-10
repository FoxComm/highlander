package consumer.activity

import java.time.Instant

import scala.concurrent.ExecutionContext

import org.json4s.JsonAST.{JInt, JNothing}
import org.json4s.jackson.JsonMethods._

final case class CustomerConnector() extends ActivityConnector {
  val dimension = "customer"
  def process(offset: Long, activity: Activity)(implicit ec: ExecutionContext) : Seq[Connection] = {
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
