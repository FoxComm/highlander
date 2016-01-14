package consumer.activity

import java.time.Instant

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.json4s.JsonAST.{JObject, JInt, JNothing}
import org.json4s.jackson.JsonMethods._

final case class CustomerConnector()(implicit ec: ExecutionContext) extends ActivityConnector {
  val dimension = "customer"
  def process(offset: Long, activity: Activity) : Future[Seq[Connection]] = Future {
    activity.activityType match {
      case "customer_updated" ⇒  customerUpdated(activity)
      case _ ⇒ mightHaveCustomer(activity)
    }
  }

  def createConnection(customerId: BigInt, activityId: Int) : Connection = {
    Connection(
      dimension = dimension,
      objectId = customerId,
      data = JNothing,
      activityId = activityId)
  }

  private def mightHaveCustomer(activity : Activity) : Seq[Connection] = {
    activity.data \ "customer" \ "id" match {
      case JInt(customerId) ⇒  Seq(createConnection(customerId, activity.id))
      case _ ⇒ Seq.empty
    }
  }

  private def customerUpdated(activity : Activity) : Seq[Connection] = {
    activity.data \ "oldInfo" \ "id" match {
      case JInt(customerId) ⇒  Seq(createConnection(customerId, activity.id))
      case _ ⇒ Seq.empty
    }
  }
}
