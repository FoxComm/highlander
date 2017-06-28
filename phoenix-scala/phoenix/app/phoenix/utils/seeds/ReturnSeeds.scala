package phoenix.utils.seeds

import core.db._
import phoenix.models.account.Scope
import phoenix.models.returns._
import phoenix.models.{Note, Notes}
import phoenix.utils.aliases._

import scala.concurrent.ExecutionContext.Implicits.global

trait ReturnSeeds {

  def createReturns(implicit au: AU): DbResultT[Unit] =
    for {
      ids ← * <~ ReturnLineItems.createAllReturningIds(returnLineItems)
      _ ← * <~ ReturnLineItemSkus.createAll(returnLineItemSkus.zip(ids).map {
           case (sku, id) ⇒ sku.copy(id = id)
         })
      _ ← * <~ Notes.createAll(returnNotes)
    } yield ()

  def createReturnReasons(implicit ec: EC): DbResultT[Option[Int]] =
    ReturnReasons.createAll(returnReasons)

  def rma =
    Return(orderId = 1, orderRef = "", returnType = Return.Standard, state = Return.Pending, accountId = 1)

  def returnLineItemSkus = Seq(
    ReturnLineItemSku(id = 0, returnId = 1, quantity = 1, skuId = 1, skuShadowId = 1),
    ReturnLineItemSku(id = 0, returnId = 1, quantity = 2, skuId = 2, skuShadowId = 2)
  )

  def returnLineItems =
    Seq(
      ReturnLineItem(id = 0, returnId = 1, reasonId = 12, originType = ReturnLineItem.SkuItem),
      ReturnLineItem(id = 0, returnId = 1, reasonId = 12, originType = ReturnLineItem.SkuItem)
    )

  def returnNotes(implicit au: AU): Seq[Note] = {
    def newNote(body: String) =
      Note(referenceId = 1, referenceType = Note.Return, storeAdminId = 1, body = body, scope = Scope.current)
    Seq(
      newNote("This customer is a donkey."),
      newNote("No, seriously."),
      newNote("Like, an actual donkey."),
      newNote("How did a donkey even place an order on our website?")
    )
  }

  def returnReasons =
    Seq(
      // Return reasons
      ReturnReason(name = "Product Return", reasonType = ReturnReason.BaseReason, rmaType = Return.Standard),
      ReturnReason(name = "Damaged Product", reasonType = ReturnReason.BaseReason, rmaType = Return.Standard),
      ReturnReason(name = "Return to Sender",
                   reasonType = ReturnReason.BaseReason,
                   rmaType = Return.Standard),
      ReturnReason(name = "Not Delivered", reasonType = ReturnReason.BaseReason, rmaType = Return.CreditOnly),
      ReturnReason(name = "Foreign Freight Error",
                   reasonType = ReturnReason.BaseReason,
                   rmaType = Return.CreditOnly),
      ReturnReason(name = "Late Delivery", reasonType = ReturnReason.BaseReason, rmaType = Return.CreditOnly),
      ReturnReason(name = "Sales Tax Error",
                   reasonType = ReturnReason.BaseReason,
                   rmaType = Return.CreditOnly),
      ReturnReason(name = "Shipping Charges Error",
                   reasonType = ReturnReason.BaseReason,
                   rmaType = Return.CreditOnly),
      ReturnReason(name = "Wrong Product", reasonType = ReturnReason.BaseReason, rmaType = Return.CreditOnly),
      ReturnReason(name = "Mis-shipment", reasonType = ReturnReason.BaseReason, rmaType = Return.CreditOnly),
      ReturnReason(name = "Failed Capture",
                   reasonType = ReturnReason.BaseReason,
                   rmaType = Return.RestockOnly),
      // Product return codes
      ReturnReason(name = "Doesn't fit",
                   reasonType = ReturnReason.ProductReturnCode,
                   rmaType = Return.Standard),
      ReturnReason(name = "Don't like",
                   reasonType = ReturnReason.ProductReturnCode,
                   rmaType = Return.Standard),
      ReturnReason(name = "Doesn't look like picture",
                   reasonType = ReturnReason.ProductReturnCode,
                   rmaType = Return.Standard),
      ReturnReason(name = "Wrong color",
                   reasonType = ReturnReason.ProductReturnCode,
                   rmaType = Return.Standard),
      ReturnReason(name = "Not specified",
                   reasonType = ReturnReason.ProductReturnCode,
                   rmaType = Return.Standard)
    )
}
