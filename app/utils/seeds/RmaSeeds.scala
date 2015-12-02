package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.{Note, Notes, Rma, RmaLineItem, RmaLineItemSku, RmaLineItemSkus, RmaLineItems, RmaReason, RmaReasons, Rmas}
import utils.DbResultT._
import utils.DbResultT.implicits._

trait RmaSeeds {

  def createRmas: DbResultT[Unit] = for {
    _ ← * <~ RmaReasons.createAll(rmaReasons)
    _ ← * <~ Rmas.create(rma)
    _ ← * <~ RmaLineItemSkus.createAll(rmaLineItemSkus)
    _ ← * <~ RmaLineItems.createAll(rmaLineItems)
    _ ← * <~ Notes.createAll(rmaNotes)
  } yield {}

  def rma = Rma(orderId = 1, orderRefNum = "", rmaType = Rma.Standard, status = Rma.Pending, customerId = 1)

  def rmaLineItemSkus = Seq(
    RmaLineItemSku(id = 0, rmaId = 1, skuId = 1),
    RmaLineItemSku(id = 0, rmaId = 1, skuId = 2)
  )

  def rmaLineItems = Seq(
    RmaLineItem(id = 0, rmaId = 1, reasonId = 12, originId = 1, originType = RmaLineItem.SkuItem,
      inventoryDisposition = RmaLineItem.Putaway),
    RmaLineItem(id = 0, rmaId = 1, reasonId = 12, originId = 2, originType = RmaLineItem.SkuItem,
      inventoryDisposition = RmaLineItem.Putaway)
  )

  def rmaNotes: Seq[Note] = {
    def newNote(body: String) = Note(referenceId = 1, referenceType = Note.Rma, storeAdminId = 1, body = body)
    Seq(
      newNote("This customer is a donkey."),
      newNote("No, seriously."),
      newNote("Like, an actual donkey."),
      newNote("How did a donkey even place an order on our website?")
    )
  }

  def rmaReasons = Seq(
    // Return reasons
    RmaReason(name = "Product Return", reasonType = RmaReason.BaseReason, rmaType = Rma.Standard),
    RmaReason(name = "Damaged Product", reasonType = RmaReason.BaseReason, rmaType = Rma.Standard),
    RmaReason(name = "Return to Sender", reasonType = RmaReason.BaseReason, rmaType = Rma.Standard),
    RmaReason(name = "Not Delivered", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
    RmaReason(name = "Foreign Freight Error", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
    RmaReason(name = "Late Delivery", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
    RmaReason(name = "Sales Tax Error", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
    RmaReason(name = "Shipping Charges Error", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
    RmaReason(name = "Wrong Product", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
    RmaReason(name = "Mis-shipment", reasonType = RmaReason.BaseReason, rmaType = Rma.CreditOnly),
    RmaReason(name = "Failed Capture", reasonType = RmaReason.BaseReason, rmaType = Rma.RestockOnly),
    // Product return codes
    RmaReason(name = "Doesn't fit", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard),
    RmaReason(name = "Don't like", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard),
    RmaReason(name = "Doesn't look like picture", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard),
    RmaReason(name = "Wrong color", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard),
    RmaReason(name = "Not specified", reasonType = RmaReason.ProductReturnCode, rmaType = Rma.Standard)
  )

}
