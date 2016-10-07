# Discount Algebra

To set qualifier and offers, update `offer` and `qualifier` attributes of discount when updating promotion.

You can have multiple qualifiers / offers in a single discount. They'll be joined using `And` rule.

Qualifier and offers, pointing to specific items, have `search` reference field, which can handle multiple references. If there are multiple references, at least one must match items from cart (`inAnyOf` rule).

Jump to: 
* [Qualifiers](#qualifiers)
* [Offers](#offers)
* [Links](#links)

## Qualifiers

Available qualifiers:

* `orderAny` - buy anything
* `orderTotalAmount` - spend over **X** USD
* `orderNumUnits` - buy any **X** items
* `itemsAny` - spend anything on items from shared search **Y**
* `itemsTotalAmount` - spend over **X** on items from shared search **Y**
* `itemsNumUnits` - buy **X** any items from shared search **Y**
* `customerDynamicGroup` - customer must be present in shared search **Y**

Example storage format:

```json
{
	"orderAny": {},
	"orderTotalAmount": {
		"totalAmount": 255
	},
	"orderNumUnits": {
		"numUnits": 3
	},
	"itemsAny": {
		"search": [
			{"productSearchId": 1},
			{"productSearchId": 2},
			{"productSearchId": 3}
		]
	},
	"itemsTotalAmount": {
		"totalAmount": 255,
		"search": [
			{"productSearchId": 1},
			{"productSearchId": 2},
			{"productSearchId": 3}
		]
	},
	"itemsNumUnits": {
		"numUnits": 3,
		"search": [
			{"productSearchId": 1},
			{"productSearchId": 2},
			{"productSearchId": 3}
		]
	},
	"customerDynamicGroup": {
		"search": {
			"customerSearchId": 1
		}
	}
}
```

## Offers

Available offers:

* `orderPercentOff` - **X%** off your order
* `orderAmountOff` - **X** USD off your order
* `itemPercentOff` - **X%** off a single item from shared search **Y**
* `itemAmountOff` - **X** USD off a single item from shared search **Y**
* `itemsPercentOff` - **X%** off all items from shared search **Y**
* `itemsAmountOff` - **X** USD off all items from shared search **Y**
* `freeShipping` - free shipping
* `discountedShipping` - **X** USD off your shipping cost.
* `setPrice` - fixed price **X** USD for **Y** items from shared search **Z**

Example storage format:

```json
{
	"orderPercentOff": {
		"discount": 30
	},
	"orderAmountOff": {
		"discount": 300
	},
	"itemPercentOff": {
		"discount": 30,
		"search": [
			{"productSearchId": 1},
			{"productSearchId": 2},
			{"productSearchId": 3}
		]
	},
	"itemAmountOff": {
		"discount": 300,
		"search": [
			{"productSearchId": 1},
			{"productSearchId": 2},
			{"productSearchId": 3}
		]
	},
	"itemsPercentOff": {
		"discount": 30,
		"search": [
			{"productSearchId": 1},
			{"productSearchId": 2},
			{"productSearchId": 3}
		]
	},
	"itemsAmountOff": {
		"discount": 300,
		"search": [
			{"productSearchId": 1},
			{"productSearchId": 2},
			{"productSearchId": 3}
		]
	},
	"freeShipping": {},
	"discountedShipping": {
		"discount": 5
	},
	"setPrice": {
		"setPrice": 30,
		"numUnits": 5,
		"search": [
			{"productSearchId": 1},
			{"productSearchId": 2},
			{"productSearchId": 3}
		]
	}
}
```

## Links

* [Discounts Overview & Business Logic](https://docs.google.com/document/d/1a4GFqIOR191QmpSO4oNJQX74QxOYv_gZoaBvc6bV6YE/edit)

