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
trait Qualifier {
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

case object OrderAnyQualifier extends Qualifier // MVP
case class OrderTotalAmountQualifier(totalAmount: Int) extends Qualifier // MVP
case class OrderNumUnitsQualifier(numUnits: Int) extends Qualifier

case class ItemsAnyQualifier(referenceId: Int,
  referenceType: QualifierReferenceType) extends Qualifier // MVP
case class ItemsTotalAmount(totalAmount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends Qualifier
case class ItemsNumUnits(numUnits: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends Qualifier
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
case class OrderPercentOffOffer(discount: Int) extends Offer // MVP
case class OrderAmountOffOffer(amount: Int) extends Offer

case class ItemsSinglePercentOffOffer(discount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends Offer
case class ItemsSingleAmountOffOffer(amount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends Offer
case class ItemsSelectPercentOffOffer(discount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends Offer // MVP
case class ItemsSelectAmountOffOffer(amount: Int, referenceId: Int,
  referenceType: QualifierReferenceType) extends Offer

case object FreeShippingOffer extends Offer // MVP
case class DiscountedShipppingOffer(fixedPrice: Int) extends Offer  
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
      // Handle extraction failure somehow...
      case (OrderPromotion, "orderAny") => Xor.Right(json.extract[OrderAnyQualifier])
      case (ItemsPromotion, "itemsAny") => Xor.Right(json.extract[ItemsAnyQualifier])
      // ...
      case _ => Xor.Left(UnknownQualifierFailure(...))
    }
  }
}
```
