package utils.seeds

import concepts.discounts.offers._
import concepts.discounts.qualifiers._
import models.objects._
import models.discount._
import models.product.SimpleContext
import payloads.{CreateDiscount, CreateDiscountForm, CreateDiscountShadow}
import utils.db._
import utils.db.DbResultT._

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

trait DiscountSeeds {

  def createDiscounts(implicit db: Database): DbResultT[Seq[BaseDiscount]] = for {
    context   ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
    results   ← * <~ DbResultT.sequence(discounts.map { case (title, payload) ⇒
      insertDiscount(title, payload, context)
    })
  } yield results

  def insertDiscount(title: String, payload: CreateDiscount, context: ObjectContext)
    (implicit db: Database): DbResultT[BaseDiscount] = for {
    form     ← * <~ ObjectForm(kind = Discount.kind, attributes = payload.form.attributes)
    shadow   ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
    ins      ← * <~ ObjectUtils.insert(form, shadow)
    discount ← * <~ Discounts.create(Discount(contextId = context.id, formId = ins.form.id,
      shadowId = ins.shadow.id, commitId = ins.commit.id))
  } yield BaseDiscount(title = title, discountId = discount.id, formId = ins.form.id, shadowId = ins.shadow.id)

  def discounts: Seq[(String, CreateDiscount)] = Seq(
    createDiscount(title = "Get 5$ off any order",
      qualifierPair = QualifierPair(OrderAny, OrderAnyQualifier),
      offerPair = OfferPair(OrderAmountOff, OrderAmountOffer(500))
    ),
    createDiscount(title = "Get 25% off when you spend more than 100$",
      qualifierPair = QualifierPair(OrderTotalAmount, OrderTotalAmountQualifier(10000)),
      offerPair = OfferPair(OrderPercentOff, OrderPercentOffer(25))
    ),
    createDiscount(title = "Get 5$ shipping when you have more than 5 items in order",
      qualifierPair = QualifierPair(OrderNumUnits, OrderNumUnitsQualifier(500)),
      offerPair = OfferPair(DiscountedShipping, DiscountedShippingOffer(500))
    ),
    createDiscount(title = "Get FREE shipping when you have more than 10 items in order",
      qualifierPair = QualifierPair(OrderNumUnits, OrderNumUnitsQualifier(10)),
      offerPair = OfferPair(FreeShipping, FreeShippingOffer)
    ),
    createDiscount(title = "Pay 2000$ if your total amount is more than 2016", // TBD: Make something more realistic
      qualifierPair = QualifierPair(OrderNumUnits, OrderTotalAmountQualifier(201600)),
      offerPair = OfferPair(SetPrice, SetPriceOffer(200000))
    )
  )

  def createDiscount(title: String, qualifierPair: QualifierPair, offerPair: OfferPair): (String, CreateDiscount) = {
    val discountForm = BaseDiscountForm(title, qualifierPair, offerPair)
    val discountShadow = BaseDiscountShadow(discountForm)
    val payload = CreateDiscount(form = CreateDiscountForm(attributes = discountForm.form),
      shadow = CreateDiscountShadow(attributes = discountShadow.shadow))

    (title, payload)
  }
}
