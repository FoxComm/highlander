package phoenix.models.cord

import core.db.FoxModel
import core.utils.Money.Currency

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
