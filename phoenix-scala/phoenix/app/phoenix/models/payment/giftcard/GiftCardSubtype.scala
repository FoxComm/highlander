package phoenix.models.payment.giftcard

import core.db._
import phoenix.models.payment.giftcard.GiftCard.OriginType
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class GiftCardSubtype(id: Int = 0, title: String, originType: OriginType)
    extends FoxModel[GiftCardSubtype]

object GiftCardSubtype {}

class GiftCardSubtypes(tag: Tag) extends FoxTable[GiftCardSubtype](tag, "gift_card_subtypes") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def title      = column[String]("title")
  def originType = column[OriginType]("origin_type")

  def * = (id, title, originType) <> ((GiftCardSubtype.apply _).tupled, GiftCardSubtype.unapply)
}

object GiftCardSubtypes
    extends FoxTableQuery[GiftCardSubtype, GiftCardSubtypes](new GiftCardSubtypes(_))
    with ReturningId[GiftCardSubtype, GiftCardSubtypes] {

  val returningLens: Lens[GiftCardSubtype, Int] = lens[GiftCardSubtype].id

  object scope {
    implicit class OriginTypeQuerySeqConversions(q: QuerySeq) {
      def customerPurchases: QuerySeq = q.byOriginType(GiftCard.CustomerPurchase)
      def csrAppeasements: QuerySeq   = q.byOriginType(GiftCard.CsrAppeasement)
      def fromStoreCredits: QuerySeq  = q.byOriginType(GiftCard.FromStoreCredit)
      def rmaProcesses: QuerySeq      = q.byOriginType(GiftCard.RmaProcess)

      def byOriginType(originType: OriginType): QuerySeq =
        q.filter(_.originType === (originType: OriginType))
    }
  }
}
