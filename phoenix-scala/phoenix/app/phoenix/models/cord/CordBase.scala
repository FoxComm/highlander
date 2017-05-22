package phoenix.models.cord

import utils.Money.Currency
import utils.db.FoxModel

trait CordBase[A <: FoxModel[A]] extends FoxModel[A] { self: A ⇒

  def referenceNumber: String
  def refNum: String = referenceNumber

  def accountId: Int
  def currency: Currency

  def subTotal: Int
  def shippingTotal: Int
  def adjustmentsTotal: Int
  def taxesTotal: Int
  def grandTotal: Int
}
