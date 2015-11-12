package models

import com.stripe.model.{Customer ⇒ StripeCustomer}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{TableQueryWithId, GenericTable, ModelWithIdParameter}
import utils.Money._
import utils.Slick.implicits._

final case class RmaPayment(id: Int = 0, rmaId: Int = 0, amount: Option[Int] = None,
  currency: Currency = Currency.USD, paymentMethodId: Int, paymentMethodType: PaymentMethod.Type)
  extends ModelWithIdParameter[RmaPayment] {

  def isCreditCard:   Boolean = paymentMethodType == PaymentMethod.CreditCard
  def isGiftCard:     Boolean = paymentMethodType == PaymentMethod.GiftCard
  def isStoreCredit:  Boolean = paymentMethodType == PaymentMethod.StoreCredit
}

object RmaPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, rma: Rma): RmaPayment =
    RmaPayment(rmaId = rma.id, paymentMethodId = 1, paymentMethodType = PaymentMethod.CreditCard)

  def build(method: PaymentMethod): RmaPayment = method match {
    case gc: GiftCard ⇒
      RmaPayment(paymentMethodId = gc.id, paymentMethodType = PaymentMethod.GiftCard)
    case cc: CreditCard ⇒
      RmaPayment(paymentMethodId = cc.id, paymentMethodType = PaymentMethod.CreditCard)
    case sc: StoreCredit ⇒
      RmaPayment(paymentMethodId = sc.id, paymentMethodType = PaymentMethod.StoreCredit)
  }

}

class RmaPayments(tag: Tag) extends GenericTable.TableWithId[RmaPayment](tag, "rma_payments") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def paymentMethodId = column[Int]("payment_method_id")
  def paymentMethodType = column[PaymentMethod.Type]("payment_method_type")
  def amount = column[Option[Int]]("amount")
  def currency = column[Currency]("currency")

  def * = (id, rmaId, amount, currency, paymentMethodId, paymentMethodType) <> ((RmaPayment.apply _).tupled,
    RmaPayment.unapply )

  def rma = foreignKey(Rmas.tableName, rmaId, Rmas)(_.id)
}

object RmaPayments extends TableQueryWithId[RmaPayment, RmaPayments](
  idLens = GenLens[RmaPayment](_.id)
)(new RmaPayments(_)){

  import models.{PaymentMethod ⇒ Pay}

  def findAllByOrderId(id: Int): QuerySeq =
    filter(_.rmaId === id)

  def findAllStoreCredit: QuerySeq =
    filter(_.paymentMethodType === (Pay.StoreCredit: Pay.Type))

  def findAllPaymentsFor(rmaId: Int): Query[(RmaPayments, CreditCards), (RmaPayment, CreditCard), Seq] = {
    for {
      payments ← this.filter(_.rmaId === rmaId)
      cards    ← CreditCards if cards.id === payments.id
    } yield (payments, cards)
  }

  def findAllCreditCardsForOrder(rmaId: Rep[Int]): QuerySeq =
    filter(_.rmaId === rmaId).filter(_.paymentMethodType === (Pay.CreditCard: Pay.Type))

  object scope {
    implicit class RmaPaymentsQuerySeqConversions(q: QuerySeq) {
      def giftCards:    QuerySeq = q.byType(Pay.GiftCard)
      def creditCards:  QuerySeq = q.byType(Pay.CreditCard)
      def storeCredits: QuerySeq = q.byType(Pay.StoreCredit)

      def byType(pmt: Pay.Type): QuerySeq = filter(_.paymentMethodType === (pmt: Pay.Type))
    }
  }
}