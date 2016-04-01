# Discount Algebra

For more details, please refer to the:
* Code in `app/code/concepts/discounts`
* Unit tests in `test/unit/concepts`

## Qualifier storage format

```json
[
	{
		"qualifierType": "orderAny",
		"attributes": {}
	},
	{
		"qualifierType": "orderTotalAmount",
		"attributes": {
			"totalAmount": 255
		}
	}
]
```

## Offer storage format

```json
[
	{
		"offerType": "freeShipping",
		"attributes": {}
	},
	{
		"offerType": "orderPercentOff",
		"attributes": {
			"discount": 30
		}
	}
]
```
