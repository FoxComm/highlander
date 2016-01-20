package consumer.activity

import scala.concurrent.{ExecutionContext, Future}

import org.json4s.JsonAST.{JInt, JNothing}

final case class CustomerConnector()(implicit ec: ExecutionContext) extends ActivityConnector {
  val dimension = "customer"

  def process(offset: Long, activity: Activity): Future[Seq[Connection]] = Future {
    val customerIds = byContextUserType(activity) ++: byCustomerData(activity) ++:
      byCustomerUpdatedActivity(activity)
    customerIds.distinct.map(createConnection(_, activity.id))
  }

  def createConnection(customerId: String, activityId: Int): Connection = {
    Connection(
      dimension = dimension,
      objectId = customerId,
      data = JNothing,
      activityId = activityId)
  }

  private def byContextUserType(activity: Activity): Seq[String] = {
    activity.context.userType match {
      case "customer" ⇒ Seq(activity.context.userId.toString)
      case _          ⇒ Seq.empty
    }
  }

  private def byCustomerData(activity: Activity): Seq[String] = {
    activity.data \ "customer" \ "id" match {
      case JInt(customerId) ⇒ Seq(customerId.toString)
      case _                ⇒ Seq.empty
    }
  }

  private def byCustomerUpdatedActivity(activity: Activity): Seq[String] = {
    (activity.activityType, activity.data \ "oldInfo" \ "id") match {
      case ("customer_updated", JInt(customerId)) ⇒ Seq(customerId.toString)
      case _                                      ⇒ Seq.empty
    }
  }
}
