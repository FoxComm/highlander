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
sealed trait QualifierType
case object OrderAnyQualifier extends QualifierType
case class OrderTotalAmountQualifier(totalAmount: Int) extends QualifierType
case class OrderNumUnitsQualifier(numUnits: Int) extends QualifierType

case class ItemsAnyQualifier(referenceId: Int, referenceType: PromotionType)
  extends QualifierType
case class ItemsTotalAmount(totalAmount: Int, referenceId: Int,
  referenceType: PromotionType) extends QualifierType
case class ItemsNumUnits(numUnits: Int, referenceId: Int,
  referenceType: PromotionType) extends QualifierType
```

For 2-week MVP, it's necessary to implement logic only for:
* `OrderAnyQualifier`
* `OrderTotalAmountQualifier`
* `ItemsAnyQualifier`

## Offers

Offers define how price is substracted from order or line items.
