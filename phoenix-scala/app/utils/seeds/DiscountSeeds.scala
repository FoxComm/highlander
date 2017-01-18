package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.account._
import models.discount._
import models.discount.offers._
import models.discount.qualifiers._
import models.objects._
import models.product.SimpleContext
import models.sharedsearch.SharedSearch
import payloads.DiscountPayloads._
import utils.aliases._
import utils.db._

// TODO: migrate to new payloads. // narma  22.09.16
object DiscountSeeds {
  case class CreateDiscountForm(attributes: Json)
  case class CreateDiscountShadow(attributes: Json)
  case class CreateDiscount(form: CreateDiscountForm,
                            shadow: CreateDiscountShadow,
                            scope: Option[String] = None)

  case class UpdateDiscountForm(attributes: Json)

  case class UpdateDiscountShadow(attributes: Json)

  case class UpdateDiscount(form: UpdateDiscountForm, shadow: UpdateDiscountShadow)
}

trait DiscountSeeds {
  import DiscountSeeds._

  def createDiscounts(search: SharedSearch)(implicit db: DB,
                                            au: AU): DbResultT[Seq[BaseDiscount]] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      results ← * <~ discounts(search).map {
        case (title, payload) ⇒
          insertDiscount(title, payload, context)
      }
    } yield results

  def insertDiscount(title: String, payload: CreateDiscount, context: ObjectContext)(
      implicit db: DB,
      au: AU): DbResultT[BaseDiscount] =
    for {
      scope  ← * <~ Scope.resolveOverride(payload.scope)
      form   ← * <~ ObjectForm(kind = Discount.kind, attributes = payload.form.attributes)
      shadow ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
      ins    ← * <~ ObjectUtils.insert(form, shadow, schema = None)
      discount ← * <~ Discounts.create(
        Discount(scope = scope,
                 contextId = context.id,
                 formId = ins.form.id,
                 shadowId = ins.shadow.id,
                 commitId = ins.commit.id))
    } yield
      BaseDiscount(title = title,
                   discountId = discount.id,
                   formId = ins.form.id,
                   shadowId = ins.shadow.id)

  def productSearch(search: SharedSearch): Seq[ProductSearch] =
    Seq(ProductSearch(productSearchId = search.id))

  def qualifiers(search: SharedSearch): Seq[Qualifier] = Seq(
    OrderAnyQualifier,
    OrderTotalAmountQualifier(1000),
    OrderNumUnitsQualifier(2),
    ItemsAnyQualifier(productSearch(search)),
    ItemsTotalAmountQualifier(1500, productSearch(search)),
    ItemsNumUnitsQualifier(2, productSearch(search))
  )

  def offers(search: SharedSearch): Seq[Offer] = Seq(
    OrderAmountOffer(3000),
    OrderPercentOffer(30),
    ItemAmountOffer(1000, productSearch(search)),
    ItemPercentOffer(50, productSearch(search)),
    ItemsAmountOffer(3000, productSearch(search)),
    ItemsPercentOffer(30, productSearch(search)),
    FreeShippingOffer,
    DiscountedShippingOffer(500),
    SetPriceOffer(2500, 2, productSearch(search))
  )

  def discounts(search: SharedSearch): Seq[(String, CreateDiscount)] =
    for (q ← qualifiers(search); o ← offers(search)) yield createDiscount(q, o)

  def createDiscount(qualifier: Qualifier, offer: Offer): (String, CreateDiscount) = {
    val discountTitle  = DiscountTitles.getDiscountTitle(qualifier, offer)
    val discountForm   = BaseDiscountForm(discountTitle, qualifier, offer)
    val discountShadow = BaseDiscountShadow(discountForm)
    val payloadForm    = CreateDiscountForm(attributes = discountForm.form)
    val payloadShadow  = CreateDiscountShadow(attributes = discountShadow.shadow)
    val payload        = CreateDiscount(form = payloadForm, shadow = payloadShadow)
    (discountTitle, payload)
  }
}

object DiscountTitles {

  // Qualifiers
  val orderAny         = "when you buy anything"
  val orderTotalAmount = "when you spend over %s"
  val orderNumUnits    = "when you buy any %d items"
  val itemsAny         = "when you spend anything on glasses"
  val itemsTotalAmount = "when you spend over %s on glasses"
  val itemsNumUnits    = "when you buy any %d glasses"

  // Offers
  val orderAmountOff   = "Get %s off your order"
  val orderPercentOff  = "Get %s off your order"
  val itemAmountOff    = "Get %s off one pair of glasses"
  val itemPercentOff   = "Get %s off one pair of glasses"
  val itemsAmountOff   = "Get %s off all glasses"
  val itemsPercentOff  = "Get %s off all glasses"
  val freeShipping     = "Get FREE shipping"
  val discountShipping = "Get %s off shipping"
  val setPrice         = "Pay only %s for %d pair(s) of glasses"

  def getDiscountTitle(qualifier: Qualifier, offer: Offer): String =
    getOfferPrefix(offer) + " " + getQualifierSuffix(qualifier)

  private def getQualifierSuffix(qualifier: Qualifier): String = qualifier match {
    case OrderAnyQualifier                   ⇒ orderAny
    case OrderTotalAmountQualifier(total)    ⇒ orderTotalAmount.format(dollars(total))
    case OrderNumUnitsQualifier(units)       ⇒ orderNumUnits.format(units)
    case ItemsAnyQualifier(_)                ⇒ itemsAny
    case ItemsTotalAmountQualifier(total, _) ⇒ itemsTotalAmount.format(dollars(total))
    case ItemsNumUnitsQualifier(units, _)    ⇒ itemsNumUnits.format(units)
  }

  private def getOfferPrefix(offer: Offer): String = offer match {
    case OrderAmountOffer(value)        ⇒ orderAmountOff.format(dollars(value))
    case OrderPercentOffer(value)       ⇒ orderPercentOff.format(percents(value))
    case ItemAmountOffer(value, _)      ⇒ itemAmountOff.format(dollars(value))
    case ItemPercentOffer(value, _)     ⇒ itemPercentOff.format(percents(value))
    case ItemsAmountOffer(value, _)     ⇒ itemsAmountOff.format(dollars(value))
    case ItemsPercentOffer(value, _)    ⇒ itemsPercentOff.format(percents(value))
    case FreeShippingOffer              ⇒ freeShipping
    case DiscountedShippingOffer(value) ⇒ discountShipping.format(dollars(value))
    case SetPriceOffer(value, units, _) ⇒ setPrice.format(dollars(value), units)
  }

  private def dollars(cents: Int): String = "$%.2f".format(cents.toDouble / 100)

  private def percents(value: Int): String = value.toString + "%"
}
