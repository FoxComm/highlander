# Discount Algebra

**WORK IN PROGRESS**

Discounts system is based on a concept of `Promotion`, which is a combination of:

* Qualifier
* Offer

## Basics

Each promotion applies it's discounts either to an order or to it's specific
line items:

```scala
sealed trait PromotionType
case object OrderPromotion extends PromotionType
case object ItemsPromotion extends PromotionType
```

## Qualifier

Qualifier checks if order or item is applicable to it's promotion.

```scala
trait Qualifier {
  // TBD - Define base methods
  val promotionType: PromotionType
  val qualifierType: QualifierType
}
```

### Qualifier types

```scala
sealed trait QualifierReferenceType
case object SharedSearch extends QualifierReferenceType
case object Product extends QualifierReferenceType
case object Sku extends QualifierReferenceType

sealed trait QualifierType
case object OrderAnyQualifier extends QualifierType // MVP
case class OrderTotalAmountQualifier(totalAmount: Int) extends QualifierType // MVP
case class OrderNumUnitsQualifier(numUnits: Int) extends QualifierType
case class ItemsAnyQualifier(referenceId: Int,
  referenceType: QualifierReferenceType) extends QualifierType // MVP
case class ItemsTotalAmount(totalAmount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends QualifierType
case class ItemsNumUnits(numUnits: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends QualifierType
```

## Offers

Offers define how price is substracted from order or line items.

```scala
trait Offer {
  // TBD
}
```

### Offer types

```scala
sealed trait OfferType
case class OrderTotalPercentOff(discount: Int) extends OfferType // MVP
case class OrderTotalAmountOff(amount: Int) extends OfferType
case class ItemsSinglePercentOff(discount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends OfferType
case class ItemsSingleAmountOff(amount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends OfferType
case class ItemsSelectPercentOff(discount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends OfferType // MVP
case class ItemsSelectAmountOff(amount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends OfferType
case object FreeShipping extends OfferType // MVP
case class DiscountedShippping(fixedPrice: Int) extends OfferType  
```

## Factories

We will store qualifier's type and it's attributes in database somehow, so we'll
need to define some kind of factory method:

```scala
object QualifierFactory {
  def factory(qualifierType: QualifierType, attributes: Json): Qualifier = {
    qualifierType match {
      case OrderAnyQualifier => parse(attributes).extract[OrderAnyQualifier]
    }
  }
}
