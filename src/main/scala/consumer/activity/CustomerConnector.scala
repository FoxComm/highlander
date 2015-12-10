package consumer.activity

import java.time.Instant

import scala.concurrent.ExecutionContext

import org.json4s.JsonAST.{JValue, JInt, JObject, JField, JString, JNothing}
import org.json4s.jackson.JsonMethods._

class CustomerConnector extends ActivityConnector {
  val dimension = "customer"
  val supportedTypes = List("customer_info_changed")
  def process(offset: Long, activity: Activity)(implicit ec: ExecutionContext) : Seq[Connection] = {
    if(supportedTypes.contains(activity.activityType)) { 
      activity.data \ "customerId" match {
        case JInt(customerId) ⇒  Seq(createConnection(customerId, activity.id))
        case _ ⇒ Seq.empty
      }
    } else {
      Seq.empty
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
