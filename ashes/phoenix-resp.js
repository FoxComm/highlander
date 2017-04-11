// Phoenix
{
  "result": {
    "referenceNumber": "BR19847",
    "paymentState": "auth",
    "lineItems": {
      "skus": [{
        "imagePath": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg",
        "referenceNumbers": ["434aa07d-f9ab-ecfa-d508-cd39438fb092"],
        "name": "Product 6464434",
        "sku": "SKU-8081099",
        "price": 5000,
        "quantity": 1,
        "totalPrice": 5000,
        "productFormId": 21324,
        "trackInventory": true,
        "state": "pending"
      }]
    },
    "lineItemAdjustments": [{
      "adjustmentType": "orderAdjustment",
      "subtract": 2250
    }],
    "promotion": {
      "id": 221,
      "context": {
        "name": "default",
        "attributes": {
          "lang": "en",
          "modality": "desktop"
        }
      },
      "applyType": "auto",
      "attributes": {
        "name": {
          "t": "string",
          "v": "45% off after spending 10 dollars"
        },
        "tags": {
          "t": "tags",
          "v": []
        },
        "details": {
          "t": "richText",
          "v": "This offer applies after you spend over 10 dollars"
        },
        "activeTo": {
          "t": "date",
          "v": null
        },
        "activeFrom": {
          "t": "date",
          "v": "2017-01-23T18:33:43.470Z"
        },
        "description": {
          "t": "text",
          "v": "45% off full order after spending 10 dollars"
        },
        "storefrontName": {
          "t": "richText",
          "v": "45% off after spending 10 dollars"
        }
      },
      "discounts": [{
        "id": 222,
        "context": {
          "name": "default",
          "attributes": {
            "lang": "en",
            "modality": "desktop"
          }
        },
        "attributes": {
          "tags": {
            "t": "tags",
            "v": []
          },
          "offer": {
            "t": "offer",
            "v": {
              "orderPercentOff": {
                "discount": 45
              }
            }
          },
          "title": {
            "t": "string",
            "v": "Get 45% off when you spend 10 dollars"
          },
          "qualifier": {
            "t": "qualifier",
            "v": {
              "orderTotalAmount": {
                "totalAmount": 1000
              }
            }
          },
          "description": {
            "t": "richText",
            "v": "45% off when you spend over 10 dollars"
          }
        }
      }]
    },
    "totals": {
      "subTotal": 5000,
      "taxes": 0,
      "shipping": 0,
      "adjustments": 2250,
      "total": 2750
    },
    "customer": {
      "id": 11419,
      "email": "qatest2278+7280311@gmail.com",
      "name": "Customer 7280311",
      "createdAt": "2017-04-10T01:32:31.880Z",
      "disabled": false,
      "isGuest": false,
      "isBlacklisted": false,
      "totalSales": 0,
      "storeCreditTotals": {
        "availableBalance": 0,
        "currentBalance": 0
      },
      "groups": []
    },
    "shippingMethod": {
      "id": 2,
      "name": "Standard shipping (USPS)",
      "code": "STANDARD-FREE",
      "price": 0,
      "isEnabled": true
    },
    "shippingAddress": {
      "id": 4770,
      "region": {
        "id": 4177,
        "countryId": 234,
        "name": "Washington"
      },
      "name": "John Doe",
      "address1": "7500 Roosevelt Way NE",
      "address2": "Block 42",
      "city": "Seattle",
      "zip": "98115",
      "isDefault": false,
      "phoneNumber": "5038234000"
    },
    "billingAddress": {
      "id": 0,
      "region": {
        "id": 4177,
        "countryId": 234,
        "name": "Washington"
      },
      "name": "John Doe",
      "address1": "7500 Roosevelt Way NE",
      "address2": "Block 42",
      "city": "Seattle",
      "zip": "98115",
      "phoneNumber": "5038234000"
    },
    "billingCreditCardInfo": {
      "id": 6064,
      "customerId": 11419,
      "holderName": "Customer 7280311",
      "lastFour": "4444",
      "expMonth": 3,
      "expYear": 2020,
      "brand": "MasterCard",
      "address": {
        "id": 0,
        "region": {
          "id": 4177,
          "countryId": 234,
          "name": "Washington"
        },
        "name": "John Doe",
        "address1": "7500 Roosevelt Way NE",
        "address2": "Block 42",
        "city": "Seattle",
        "zip": "98115",
        "phoneNumber": "5038234000"
      },
      "type": "creditCard",
      "createdAt": "2017-04-10T01:32:41.553Z"
    },
    "paymentMethods": [{
      "id": 6064,
      "customerId": 11419,
      "holderName": "Customer 7280311",
      "lastFour": "4444",
      "expMonth": 3,
      "expYear": 2020,
      "brand": "MasterCard",
      "address": {
        "id": 0,
        "region": {
          "id": 4177,
          "countryId": 234,
          "name": "Washington"
        },
        "name": "John Doe",
        "address1": "7500 Roosevelt Way NE",
        "address2": "Block 42",
        "city": "Seattle",
        "zip": "98115",
        "phoneNumber": "5038234000"
      },
      "type": "creditCard",
      "createdAt": "2017-04-10T01:32:41.553Z"
    }],
    "orderState": "fulfillmentStarted",
    "shippingState": "fulfillmentStarted",
    "fraudScore": 7,
    "placedAt": "2017-04-10T01:32:55.695Z"
  }
}