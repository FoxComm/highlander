package phoenix.models.cord

import utils.Money.Currency
import utils.db.FoxModel

trait CordBase[A <: FoxModel[A]] extends FoxModel[A] { self: A ⇒

  def referenceNumber: String
  def refNum: String = referenceNumber

  def accountId: Int
  def currency: Currency

  def subTotal: Long
  def shippingTotal: Long
  def adjustmentsTotal: Long
  def taxesTotal: Long
  def grandTotal: Long
}
