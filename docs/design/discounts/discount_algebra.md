# Discount Algebra

**WORK IN PROGRESS**

Discounts system is based on a concept of `Promotion`, which is a combination of:

* Qualifier
* Offer

## Basics

Each promotion applies it's discounts either to an order or to it's line items:

```scala
sealed trait PromotionType
case object OrderPromotion extends PromotionType
case object ItemsPromotion extends PromotionType
```

## Qualifier

Qualifier checks if order or item is applicable to it's promotion.

```scala
trait Qualifier[T <: Qualifier[T]] {
  // TBD - Define base methods
  val promotionType: PromotionType
  val qualifierType: String // FIXME with ADTs
}
```

### Qualifier types

```scala
sealed trait QualifierReferenceType
case object SharedSearch extends QualifierReferenceType
case object Product extends QualifierReferenceType
case object Sku extends QualifierReferenceType

case object OrderAnyQualifier
  extends Qualifier[OrderAnyQualifier] // MVP
case class OrderTotalAmountQualifier(totalAmount: Int)
  extends Qualifier[OrderTotalAmountQualifier] // MVP
case class OrderNumUnitsQualifier(numUnits: Int)
  extends Qualifier[OrderNumUnitsQualifier]

case class ItemsAnyQualifier(referenceId: Int,
  referenceType: QualifierReferenceType)
  extends Qualifier[ItemsAnyQualifier] // MVP
case class ItemsTotalAmount(totalAmount: Int, referenceId: Int,
  referenceType: QualifierReferenceType)
  extends Qualifier[ItemsTotalAmount]
case class ItemsNumUnits(numUnits: Int, referenceId: Int,
  referenceType: QualifierReferenceType)
  extends Qualifier[ItemsNumUnits]
```

## Offers

Offers define how price is substracted from order or line items.

```scala
trait Offer[T <: Offer[T]] {
  // TBD
}
```

### Offer types

```scala
case class OrderPercentOffOffer(discount: Int)
  extends Offer[OrderPercentOffOffer] // MVP
case class OrderAmountOffOffer(amount: Int)
  extends Offer[OrderPercentOffOffer]

case class ItemsSinglePercentOffOffer(discount: Int, referenceId: Int,
  referenceType: QualifierReferenceType)
  extends Offer[ItemsSinglePercentOffOffer]
case class ItemsSingleAmountOffOffer(amount: Int, referenceId: Int,
  referenceType: QualifierReferenceType)
  extends Offer[ItemsSingleAmountOffOffer]
case class ItemsSelectPercentOffOffer(discount: Int, referenceId: Int,
  referenceType: QualifierReferenceType)
  extends Offer[ItemsSelectPercentOffOffer] // MVP
case class ItemsSelectAmountOffOffer(amount: Int, referenceId: Int,
  referenceType: QualifierReferenceType)
  extends Offer[ItemsSelectAmountOffOffer]

case object FreeShippingOffer
  extends Offer[FreeShippingOffer] // MVP
case class DiscountedShipppingOffer(fixedPrice: Int)
  extends Offer[DiscountedShipppingOffer]  
```

## Factories

We will store qualifier's type and it's attributes in database somehow, so we'll
need to define some kind of factory method:

```scala
object QualifierFactory {

  def factory(promoType: PromotionType, qualifierType: String,
    attributes: String): Xor[Failure, Qualifier] = {

    val json = parse(attributes)

    (promoType, qualifierType) match {
      case (OrderPromotion, "orderAny") => extract[OrderAnyQualifier](json)
      case (ItemsPromotion, "itemsAny") => extract[ItemsAnyQualifier](json)
      // ...
      case _ => Xor.Left(UnknownQualifierFailure(...))
    }
  }

  private def extract[T <: Qualifier[T]](json: JValue): Xor[Failure, Qualifier] = {
    json.extractOpt[T] match {
      case Some(q) => Xor.Right(q)
      case None    => Xor.Left(ExtractionFailure(...))
    }
  }
}
```
