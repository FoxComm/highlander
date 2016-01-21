package consumer.activity

import scala.concurrent.{ExecutionContext, Future}

import consumer.utils.JsonTransformers.extractStringSeq

import org.json4s.JsonAST.{JString, JNothing}

final case class GiftCardConnector()(implicit ec: ExecutionContext) extends ActivityConnector {
  val dimension = "gift_card"

  def process(offset: Long, activity: Activity): Future[Seq[Connection]] = Future {
    val giftCardIds = byGiftCardData(activity) ++: byBulkData(activity)
    giftCardIds.distinct.map(createConnection(_, activity.id))
  }

  def createConnection(code: String, activityId: Int): Connection = {
    Connection(
      dimension = dimension,
      objectId = code,
      data = JNothing,
      activityId = activityId)
  }

  private def byGiftCardData(activity: Activity): Seq[String] = {
    activity.data \ "giftCard" \ "code" match {
      case JString(refNum)  ⇒ Seq(refNum)
      case _                ⇒ Seq.empty
    }
  }

  private def byBulkData(activity: Activity): Seq[String] = extractStringSeq(activity.data, "giftCardCodes")
}
