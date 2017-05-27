package phoenix.models.cord

import core.db.FoxModel
import core.utils.Money.Currency

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
