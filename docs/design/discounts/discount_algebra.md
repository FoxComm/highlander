# Discount Algebra

For more details, please refer to the [Discounts Overview & Business Logic](https://docs.google.com/document/d/1a4GFqIOR191QmpSO4oNJQX74QxOYv_gZoaBvc6bV6YE/edit) design document.

## How to add qualifiers and offers of Promotion

Just update `offer` and `qualifier` attributes when updating Promotion / Discount.

Future updates:
* __Soon__: we will support an array of offers/qualifiers, so we'll use `offers` and `qualifiers` field names instead.  
* __Later__: we will support nested rules, in combination with operators like AND/OR/NOT etc.

## Qualifier examples & storage format

```json
[
	{
		"orderAny": {}
	},
	{
		"orderTotalAmount": {
			"totalAmount": 255
		}
	},
	{
		"orderNumUnits": {
			"numUnits": 3
		}
	},
	{
		"itemsAny": {
			"references": [
				{
					"referenceId": "1",
					"referenceType": "product"
				}
			]
		}
	},
	{
		"itemsTotalAmount": {
			"totalAmount": 255,
			"references": [
				{
					"referenceId": "SKU-YAX",
					"referenceType": "sku"
				}
			]
		}
	}
	{
		"itemsNumUnits": {
			"numUnits": 3,
			"references": [
				{
					"referenceId": "SKU-YAX",
					"referenceType": "sku"
				}
			]
		}
	}
]
```

## Offer examples & storage format

```json
[
	{
		"orderPercentOff": {
			"discount": 30
		}
	},
	{
		"orderAmountOff": {
			"discount": 300
		}
	},
	{
		"itemPercentOff": {
			"discount": 30,
			"references": [
				{
					"referenceId": "1",
					"referenceType": "product"
				}
			]			
		}
	},
	{
		"itemAmountOff": {
			"discount": 300,
			"references": [
				{
					"referenceId": "SKU-YAX",
					"referenceType": "sku"
				}
			]			
		}
	},	
	{
		"itemsPercentOff": {
			"discount": 30,
			"references": [
				{
					"referenceId": "1",
					"referenceType": "product"
				}
			]			
		}
	},
	{
		"itemsAmountOff": {
			"discount": 300,
			"references": [
				{
					"referenceId": "SKU-YAX",
					"referenceType": "sku"
				}
			]			
		}
	},
	{
		"freeShipping": {}
	},
	{
		"discountedShipping": {
			"setPrice": 5
		}
	},
	{
		"setPrice": {
			"setPrice": 30
		}
	}
]
```
