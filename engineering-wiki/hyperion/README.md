# Hyperion wiki

Hyperion is a mecroservice for comunications with Amawon MWS:
    * submit products
    * getting orders
    * getting some product related info


How to start and some quick info is [here](https://github.com/FoxComm/highlander/tree/add_amazon_microservice/hyperion)

## Examples

All requests should have two headers:
* `jwt` header to work with Phoenix-scala

### MWS Credentials

Next endpoints used to manage client credentials for MWS.
Client ID is taken from decoded JWT.

#### Get client credentials

*request*

```
GET /api/v1/hyperion/credentials
```

*response*

```json
{
    "seller_id": "seller_id123",
    "mws_auth_token": "token1212",
    "client_id": 123
}
```

#### Store cleint credentials

*request*

```
POST /api/v1/hyperion/credentials
```
body:

```json
{
  "seller_id": "foo",
  "mws_auth_token": "bar"
}
```

*response*

```json
{
    "seller_id": "foo",
    "mws_auth_token": "bar",
    "client_id": 222
}
```

#### Update client credentials

*request*

```
PUT /api/v1/hyperion/credentials
```
body:

```json
{
  "seller_id": "new_seller_id",
  "mws_auth_token": "new_token"
}
```

*response*

```json
{
    "seller_id": "new_seller_id",
    "mws_auth_token": "new_token",
    "client_id": 123
}
```

#### Delete client credentials

*request*

```
DELETE /api/v1/hyperion/credentials
```

*response*

If credentials found and successfully deleted `HTTP 204: NO CONTENT` will return.

If no credentials found:

```json
{
    "error": "Credentials for client 123 not found"
}
```
### MWS

The next endpoints used to manage MWS products and orders and stuff like Categories, object schemas and so on.

#### Push product to amazon

Product pushed in five steps:

* product info
* variations info (will be skipped if product has no variants)
* price info
* inventory
* images

If some steps had failed and product has been pushed one more time only failed steps will be executed then.

*request*

|name|type|description|required?|
|----|----|-----------|---------|
|purge |Boolean|If `true` will replace existing product in Amazon. All steps will be executed. Parameter can be omitted.|No|


```
POST /api/v1/hyperion/products/:product_id/push
```

```json
{
  "purge": false,
}
```

*response*

```json
{
    "product_id": 484,
    "product_feed": {
        "SubmittedDate": "2017-03-15T08:58:35+00:00",
        "FeedType": "_POST_PRODUCT_DATA_",
        "FeedSubmissionId": "50162017240",
        "FeedProcessingStatus": "_SUBMITTED_"
    },
    "price_feed": {
        "SubmittedDate": "2017-03-15T08:58:36+00:00",
        "FeedType": "_POST_PRODUCT_PRICING_DATA_",
        "FeedSubmissionId": "50163017240",
        "FeedProcessingStatus": "_SUBMITTED_"
    },
    "inventory_feed": {
        "SubmittedDate": "2017-03-15T08:58:37+00:00",
        "FeedType": "_POST_INVENTORY_AVAILABILITY_DATA_",
        "FeedSubmissionId": "50164017240",
        "FeedProcessingStatus": "_SUBMITTED_"
    },
    "images_feed": null
}
```

### Get push result for a product

*request*

```
GET /api/v1/hyperion/:product_id/result
```

*response*

```json
{
    "product_id": 484,
    "product_feed": {
        "SubmittedDate": "2017-03-15T08:58:35+00:00",
        "FeedType": "_POST_PRODUCT_DATA_",
        "FeedSubmissionId": "50162017240",
        "FeedProcessingStatus": "_SUBMITTED_"
    },
    "price_feed": {
        "SubmittedDate": "2017-03-15T08:58:36+00:00",
        "FeedType": "_POST_PRODUCT_PRICING_DATA_",
        "FeedSubmissionId": "50163017240",
        "FeedProcessingStatus": "_SUBMITTED_"
    },
    "inventory_feed": {
        "SubmittedDate": "2017-03-15T08:58:37+00:00",
        "FeedType": "_POST_INVENTORY_AVAILABILITY_DATA_",
        "FeedSubmissionId": "50164017240",
        "FeedProcessingStatus": "_SUBMITTED_"
    },
    "images_feed": null
}
```


#### Submit product feed to MWS

*request*

```
POST /api/v1/hyperion/products
```

|name|type|description|required?|
|----|----|-----------|---------|
|ids |Array|Array of products ids|Yes|
|purge |Boolean|If `true` will replace existing product. Can be omitted.|No|

body:

```json
{
  "ids":[226],
  "purge": false
}
```

*response*

```json
{
    "SubmitFeedResponse": {
        "SubmitFeedResult": {
            "FeedSubmissionInfo": {
                "SubmittedDate": "2017-02-10T09:51:38+00:00",
                "FeedType": "_POST_PRODUCT_DATA_",
                "FeedSubmissionId": "50067017207",
                "FeedProcessingStatus": "_SUBMITTED_"
            }
        },
        "ResponseMetadata": {
            "RequestId": "d23e05ec-e610-4f54-b94b-b36ac86fec3c"
        }
    }
}
```

#### Submit products feed by ASIN

```
POST /api/v1/hyperion/products/by_asin
```

|name|type|description|required?|
|----|----|-----------|---------|
|ids |Array|Array of products ids|Yes|
|purge |Boolean|If `true` will replace existing product. Can be omitted.|No|

body:

```json
{
  "ids":[226],
  "purge": false
}
```

*response*

```json
{
    "SubmitFeedResponse": {
        "SubmitFeedResult": {
            "FeedSubmissionInfo": {
                "SubmittedDate": "2017-02-10T09:51:38+00:00",
                "FeedType": "_POST_PRODUCT_DATA_",
                "FeedSubmissionId": "50067017207",
                "FeedProcessingStatus": "_SUBMITTED_"
            }
        },
        "ResponseMetadata": {
            "RequestId": "d23e05ec-e610-4f54-b94b-b36ac86fec3c"
        }
    }
}
```


#### Submit price feed to MWS

*request*

```
POST /api/v1/hyperion/prices
```
body:

```json
{
  "ids":[226]
}
```

*response*

```json
{
    "SubmitFeedResponse": {
        "SubmitFeedResult": {
            "FeedSubmissionInfo": {
                "SubmittedDate": "2017-02-10T09:52:13+00:00",
                "FeedType": "_POST_PRODUCT_PRICING_DATA_",
                "FeedSubmissionId": "50068017207",
                "FeedProcessingStatus": "_SUBMITTED_"
            }
        },
        "ResponseMetadata": {
            "RequestId": "591975bd-761e-4fe0-9385-68b8745c10f8"
        }
    }
}
```


#### Submit inventory feed to MWS

*request*

```
POST /api/v1/hyperion/inventory
```
body:

```json
{
  "inventory":[
    {"sku": "AMZ2331", "quantity": 100}
  ]
}
```

*response*

```json
{
    "SubmitFeedResponse": {
        "SubmitFeedResult": {
            "FeedSubmissionInfo": {
                "SubmittedDate": "2017-02-10T09:53:03+00:00",
                "FeedType": "_POST_INVENTORY_AVAILABILITY_DATA_",
                "FeedSubmissionId": "50070017207",
                "FeedProcessingStatus": "_SUBMITTED_"
            }
        },
        "ResponseMetadata": {
            "RequestId": "58b10f54-a747-4ea5-b0b0-d978b89a3227"
        }
    }
}
```

#### Submit images feed

*request*

```
POST /api/v1/hyperion/images
```
body:

```json
{
  "ids":[304]
}
```

*response*

```json
{
    "SubmitFeedResponse": {
        "SubmitFeedResult": {
            "FeedSubmissionInfo": {
                "SubmittedDate": "2017-03-02T09:53:03+00:00",
                "FeedType": "_POST_PRODUCT_IMAGE_DATA_",
                "FeedSubmissionId": "50070017207",
                "FeedProcessingStatus": "_SUBMITTED_"
            }
        },
        "ResponseMetadata": {
            "RequestId": "58b10f54-a747-4ea5-b0b0-d978b89a3227"
        }
    }
}
```

#### List matching products by query string

*request*

```
GET /api/v1/hyperion/products/search?q=:query_string
```

*response*

```json
{
    "ListMatchingProductsResponse": {
        "ResponseMetadata": {
            "RequestId": "3a597922-c721-4e16-bc2f-51783f49de2d"
        },
        "ListMatchingProductsResult": {
            "Products": {
              [looong_document_here]
            }
        }
    }
}
```

#### Get feed submission result

_IMPORTANT:_ Feed can be processed with warnings. In most cases with warnings your product reached MWS.

*request*

```
GET /api/v1/hyperion/submission_result/:feed_id
```

*response*

Success

```json
{
    "AmazonEnvelope": {
        "{http://www.w3.org/2001/XMLSchema-instance}noNamespaceSchemaLocation": "amzn-envelope.xsd",
        "MessageType": "ProcessingReport",
        "Message": {
            "ProcessingReport": {
                "StatusCode": "Complete",
                "ProcessingSummary": {
                    "MessagesWithWarning": "0",
                    "MessagesWithError": "0",
                    "MessagesSuccessful": "1",
                    "MessagesProcessed": "1"
                },
                "DocumentTransactionID": "50068017207"
            },
            "MessageID": "1"
        },
        "Header": {
            "MerchantIdentifier": "A2KK3Z7K1ON8YS",
            "DocumentVersion": "1.02"
        }
    }
}
```

Warning

```json
{
    "AmazonEnvelope": {
        "{http://www.w3.org/2001/XMLSchema-instance}noNamespaceSchemaLocation": "amzn-envelope.xsd",
        "MessageType": "ProcessingReport",
        "Message": {
            "ProcessingReport": {
                "StatusCode": "Complete",
                "Result": [
                    {
                        "ResultMessageCode": "99041",
                        "ResultDescription": "A value was not provided for \"brand_name\". Please provide a value for \"brand_name\". This information appears on the product detail page and helps customers evaluate products.",
                        "ResultCode": "Warning",
                        "MessageID": "1",
                        "AdditionalInfo": {
                            "SKU": "AMZ2331"
                        }
                    },
                    {
                        "ResultMessageCode": "99041",
                        "ResultDescription": "A value was not provided for \"bullet_point1\". Please provide a value for \"bullet_point1\". This information appears on the product detail page and helps customers evaluate products.",
                        "ResultCode": "Warning",
                        "MessageID": "1",
                        "AdditionalInfo": {
                            "SKU": "AMZ2331"
                        }
                    }
                ],
                "ProcessingSummary": {
                    "MessagesWithWarning": "1",
                    "MessagesWithError": "0",
                    "MessagesSuccessful": "1",
                    "MessagesProcessed": "1"
                },
                "DocumentTransactionID": "50067017207"
            },
            "MessageID": "1"
        },
        "Header": {
            "MerchantIdentifier": "A2KK3Z7K1ON8YS",
            "DocumentVersion": "1.02"
        }
    }
}
```

### Get matching product by ASIN

*request*

```
GET /api/v1/hyperion/products/find_by_asin/:asin
```

*response*

```json
{
    "GetMatchingProductResponse": {
        "ResponseMetadata": {
            "RequestId": "150d680a-0d21-42ce-a463-2d8a899fa4fe"
        },
        "GetMatchingProductResult": {
            "status": "Success",
            "Product": {
                "SalesRankings": {
                    "SalesRank": [
                        {
                            "Rank": "2792",
                            "ProductCategoryId": "wireless_display_on_website"
                        },
                        {
                            "Rank": "64",
                            "ProductCategoryId": "2407749011"
                        }
                    ]
                },
                "Relationships": {
                    "VariationParent": {
                        "Identifiers": {
                            "MarketplaceASIN": {
                                "MarketplaceId": "ATVPDKIKX0DER",
                                "ASIN": "B01LS29SP6"
                            }
                        }
                    }
                },
                "Identifiers": {
                    "MarketplaceASIN": {
                        "MarketplaceId": "ATVPDKIKX0DER",
                        "ASIN": "B01LYT95XR"
                    }
                },
                "AttributeSets": {
                    "ItemAttributes": {
                        "{http://www.w3.org/XML/1998/namespace}lang": "en-US",
                        "Title": "Apple iPhone 7 Unlocked CDMA/GSM 32GB A1660 MNAC2LL/A - US Version (Black)",
                        "Studio": "Apple",
                        "SmallImage": {
                            "Width": "75",
                            "URL": "http://ecx.images-amazon.com/images/I/41q97rMijoL._SL75_.jpg",
                            "Height": "56"
                        },
                        "Size": "32 GB",
                        "Publisher": "Apple",
                        "ProductTypeName": "WIRELESS_ACCESSORY",
                        "ProductGroup": "Wireless",
                        "PartNumber": "Unlocked 32 GB - US (Black)",
                        "PackageQuantity": "1",
                        "PackageDimensions": {
                            "Width": "3.40",
                            "Weight": "0.50",
                            "Length": "6.20",
                            "Height": "2.00"
                        },
                        "OperatingSystem": "IOS 10",
                        "Model": "Unlocked 32 GB - US (Black)",
                        "Manufacturer": "Apple",
                        "ListPrice": {
                            "CurrencyCode": "USD",
                            "Amount": "649.00"
                        },
                        "Label": "Apple",
                        "ItemDimensions": {
                            "Width": "2.64",
                            "Weight": "0.30",
                            "Length": "5.44",
                            "Height": "0.28"
                        },
                        "IsEligibleForTradeIn": "true",
                        "Feature": [
                            "Unlocked for use with the carrier of your choice. Compatible with Verizon, AT&T, T-Mobile, Tracfone, Family Mobile, Cricket, Straight Talk, and other GSM carriers worldwide; not with CDMA Sprint.",
                            "An entirely new camera system. The brightest, most colorful iPhone display ever. The fastest performance and best battery life in an iPhone. Every bit as powerful as it looks.",
                            "7MP FaceTime HD camera with Retina Flash • Splash, water, and dust resistant • 4K video recording at 30 fps and slo-mo video recording for 1080p at 120 fps • Touch ID fingerprint sensor built in",
                            "New 12MP camera, optical image stabilization, Quad-LED True Tone flash, and Live Photos • LTE Advanced up to 450 Mbps and 802.11a/b/g/n/ac Wi-Fi with MIMO • iOS 10 and iCloud",
                            "A Verizon SIM card is included but the phone is not tied to any account (discard the Verizon SIM). SIM cards must be nano SIM size to be compatible (a larger SIM can be cut down to size if needed)."
                        ],
                        "DisplaySize": "4.7",
                        "Color": "Black",
                        "Brand": "Apple",
                        "Binding": "Wireless Phone Accessory"
                    }
                }
            },
            "ASIN": "B01LYT95XR"
        }
    }
}
```

#### Get orders

_IMPORTANT:_ This endpoint will be upgraded soon. It will stay backward compatible but will have some additional params.

Params:

|name|type|description|required?|
|----|----|-----------|---------|
|created_after|String|Date orders created after:   |No|
|title |String|Product title|No|

* created_after — DateTime is ISO8601 format `%Y-%m-%dT%H:%M:%SZ`. Yes, if `last_updated_after` is not specified. Specifying both `created_after` and `last_updated_after` returns an error.
* created_before — DateTime is ISO8601 format `%Y-%m-%dT%H:%M:%SZ`. Must be later than `created_after`.
* fulfillment_channel
  * `MFN` — Merchant fullfilment
  * `AFN` – Amazon Fulfillmen
* payment_method
  * `COD` — cash on delivery
  * `CVS` — Convenience store payment
  * `Other` — Any payment method other than COD or CVS
* order_status
  * `PendingPickUp` — Amazon has not yet picked up the package from the seller.
  * `LabelCanceled` — The seller canceled the pickup.
  * `PickedUp` — Amazon has picked up the package from the seller.
  * `AtDestinationFC` — Package has arrived at the Amazon fulfillment center.
  * `Delivered` — Package has been delivered to the buyer.
  * `RejectedByBuyer` — Package has been rejected by the buyer.
  * `Undeliverable` — The package cannot be delivered.
  * `ReturnedToSeller` — The package was not delivered to the customer and was returned to the seller.
  * `Lost` — Package was lost by the carrier.
* last\_updated\_after — DateTime is ISO8601 format `%Y-%m-%dT%H:%M:%SZ`.Yes, if `created_after` is not specified. Specifying both `created_after` and `last_updated_after` returns an error. If `last_updated_after` is specified, then `buyer_email` and `seller_order_id` cannot be specified.
* last\_updated\_before — DateTime is ISO8601 format `%Y-%m-%dT%H:%M:%SZ`. Must be later than `last_updated_after`.
* buyer_email — The e-mail address of a buyer. Used to select only the orders that contain the specified e-mail address.
* seller\_order\_id — An order identifier that is specified by the seller. Not an Amazon order identifier. Used to select only the orders that match a seller-specified order identifier.
* max\_results\_per\_page — A number that indicates the maximum number of orders that can be returned per page.

Params marked with * are mandatory.

#### Get order details

*request*

```
GET /api/v1/hyperion/orders/:amazon_order_id
```

*response*

```json
{
    "GetOrderResponse": {
        "ResponseMetadata": {
            "RequestId": "17a19fe3-b8df-4862-997f-815a0f5c4c1b"
        },
        "GetOrderResult": {
            "Orders": {
                "Order": {
                    "ShippingAddress": {
                        "StateOrRegion": "WA",
                        "PostalCode": "98121-1044",
                        "Phone": "5672039430",
                        "Name": "Bree Swineford",
                        "CountryCode": "US",
                        "City": "SEATTLE",
                        "AddressLine1": "3131 ELLIOTT AVE STE 240"
                    },
                    "ShippedByAmazonTFM": "false",
                    "ShipmentServiceLevelCategory": "FreeEconomy",
                    "ShipServiceLevel": "Econ Cont US",
                    "SalesChannel": "Amazon.com",
                    "PurchaseDate": "2017-03-08T19:38:36Z",
                    "PaymentMethodDetails": {
                        "PaymentMethodDetail": "CreditCard"
                    },
                    "PaymentMethod": "Other",
                    "OrderType": "StandardOrder",
                    "OrderTotal": {
                        "CurrencyCode": "USD",
                        "Amount": "45.00"
                    },
                    "OrderStatus": "Shipped",
                    "NumberOfItemsUnshipped": "0",
                    "NumberOfItemsShipped": "1",
                    "MarketplaceId": "ATVPDKIKX0DER",
                    "LatestShipDate": "2017-03-11T07:59:59Z",
                    "LatestDeliveryDate": "2017-03-22T06:59:59Z",
                    "LastUpdateDate": "2017-03-08T21:02:24Z",
                    "IsReplacementOrder": "false",
                    "IsPrime": "false",
                    "IsPremiumOrder": "false",
                    "IsBusinessOrder": "false",
                    "FulfillmentChannel": "MFN",
                    "EarliestShipDate": "2017-03-09T08:00:00Z",
                    "EarliestDeliveryDate": "2017-03-15T07:00:00Z",
                    "BuyerName": "Zakk Pershing",
                    "BuyerEmail": "67xlr8kb83lx0t2@marketplace.amazon.com",
                    "AmazonOrderId": "111-5296499-9653858"
                }
            }
        }
    }
}
```

#### Get order items

*request*

```
GET /api/v1/hyperion/order/:amazon_order_id/items
```

*response*


```json
{
    "ListOrderItemsResponse": {
        "ResponseMetadata": {
            "RequestId": "b2ae1c4f-ec7b-4beb-ba71-eb403d50bb27"
        },
        "ListOrderItemsResult": {
            "OrderItems": {
                "OrderItem": {
                    "Title": "FoxCommerce Startup Hoodie (Small, Light Grey)",
                    "ShippingTax": {
                        "CurrencyCode": "USD",
                        "Amount": "0.00"
                    },
                    "ShippingPrice": {
                        "CurrencyCode": "USD",
                        "Amount": "0.00"
                    },
                    "ShippingDiscount": {
                        "CurrencyCode": "USD",
                        "Amount": "0.00"
                    },
                    "SellerSKU": "FOX-2131",
                    "QuantityShipped": "1",
                    "QuantityOrdered": "1",
                    "PromotionDiscount": {
                        "CurrencyCode": "USD",
                        "Amount": "0.00"
                    },
                    "OrderItemId": "63361525774498",
                    "ItemTax": {
                        "CurrencyCode": "USD",
                        "Amount": "0.00"
                    },
                    "ItemPrice": {
                        "CurrencyCode": "USD",
                        "Amount": "45.00"
                    },
                    "GiftWrapTax": {
                        "CurrencyCode": "USD",
                        "Amount": "0.00"
                    },
                    "GiftWrapPrice": {
                        "CurrencyCode": "USD",
                        "Amount": "0.00"
                    },
                    "ConditionSubtypeId": "New",
                    "ConditionId": "New",
                    "ASIN": "B06XGSRGC8"
                }
            },
            "AmazonOrderId": "111-5296499-9653858"
        }
    }
}
```

#### Get categories for ASIN

*request*

```
GET /api/v1/hyperion/products/categories/:asin
```

*response*

```json
{
    "GetProductCategoriesForASINResponse": {
        "ResponseMetadata": {
            "RequestId": "aa4fd886-5fce-4029-a28b-39c05bc6275d"
        },
        "GetProductCategoriesForASINResult": {
            "Self": {
                "ProductCategoryName": "Unlocked Cell Phones",
                "ProductCategoryId": "2407749011",
                "Parent": {
                    "ProductCategoryName": "Cell Phones",
                    "ProductCategoryId": "7072561011",
                    "Parent": {
                        "ProductCategoryName": "Categories",
                        "ProductCategoryId": "2335753011",
                        "Parent": {
                            "ProductCategoryName": "Cell Phones & Accessories",
                            "ProductCategoryId": "2335752011"
                        }
                    }
                }
            }
        }
    }
}
```

#### Search for Amazon category ID pair: `department — item-type`

|name|type|description|required?|
|----|----|-----------|---------|
|node_path |String|Query string |Yes|
|from |Integer|Used for paginate results. Default value is: `0` |No|
|size |Integer|Used for paginate results. Default value is: `10` |No|

*request*
```
GET /api/v1/hyperion/categories?node_path=t-shirt&size=2
```

**response**

```json
{
    "items": [
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Baby/Baby Boys/Clothing/Swim/Swimwear Sets/T-Shirt Sets",
            "node_id": 6259178011,
            "item_type": "infant-and-toddler-swimwear-t-shirt-sets",
            "department": "baby-boys"
        },
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Boys/Clothing/Swim/Swimwear Sets/T-Shirt Sets",
            "node_id": 6259168011,
            "item_type": "swimwear-t-shirt-sets",
            "department": "boys"
        }
    ],
    "count": 2
}
```

#### Suggest categories

|name|type|description|required?|
|----|----|-----------|---------|
|q |String|Query string |No|
|title |String|Product title|No|

Search for categories in Amazon and against Hyperion DB.
`q` — used for searching against Hyperion DB, `title` — to search in Amazon by product ASIN.

If no params passed empty result will return.


*request*

When only `q` is passed

```
GET /api/v1/hyperion/categories/suggest?q=necktie
```

*response*

```json
{
    "secondary": [
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Boys/Accessories/Neckties",
            "node_id": 5427586011,
            "item_type": "neckties",
            "department": "boys"
        },
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Men/Accessories/Neckties",
            "node_id": 2474955011,
            "item_type": "neckties",
            "department": "mens"
        },
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Boys/Accessories/Neckties",
            "node_id": 9057120011,
            "item_type": "novelty-neckties",
            "department": "boys"
        },
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Men/Accessories/Neckties",
            "node_id": 9057017011,
            "item_type": "novelty-neckties",
            "department": "mens"
        }
    ],
    "primary": null,
    "count": 4
}
```

When `q` and `title` are passed

*request*

```
GET /api/v1/hyperion/categories/suggest?q=necktie&title=Spiderman necktie
```

*response*

```json
{
    "secondary": [
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Boys/Accessories/Neckties",
            "node_id": 5427586011,
            "item_type": "neckties",
            "department": "boys"
        },
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Boys/Accessories/Neckties",
            "node_id": 9057120011,
            "item_type": "novelty-neckties",
            "department": "boys"
        },
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Men/Accessories/Neckties",
            "node_id": 9057017011,
            "item_type": "novelty-neckties",
            "department": "mens"
        }
    ],
    "primary": [
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Men/Accessories/Neckties",
            "node_id": 2474955011,
            "item_type": "neckties",
            "department": "mens"
        }
    ],
    "count": 4
}
```

When only `title` is passed

*request*

```
GET /api/v1/hyperion/categories/suggest?title=Spiderman necktie
```

*response*


```json
{
    "secondary": null,
    "primary": [
        {
            "size_opts": null,
            "node_path": "Clothing, Shoes & Jewelry/Men/Accessories/Neckties",
            "node_id": 2474955011,
            "item_type": "neckties",
            "department": "mens"
        }
    ],
    "count": 1
}
```

When no params passed

*request*

```
GET /api/v1/hyperion/categories/suggest
```

*response*

```json
{
    "secondary": null,
    "primary": null,
    "count": 0
}
```


#### Subscrube to notification queue

*request*

```
POST /api/v1/hyperion/subscribe
```

body:

```json
{
  "queue_url": "https://sqs.us-west-2.amazonaws.com/631158685056/tst"
}
```

*response*

```json
{
    "RegisterDestinationResponse": {
        "ResponseMetadata": {
            "RequestId": "23d04aca-cf23-4fd6-a4e9-9c0ad95e1fb5"
        },
        "RegisterDestinationResult": {}
    }
}
```

If already subscribed error will return:

```json
{
    "ErrorResponse": {
        "RequestId": "48347c7d-9a50-4f6b-a3c8-b43c083222a5",
        "Error": {
            "Type": "Sender",
            "Message": "An exception was thrown while attempting to Create a Destination. This can happen if the Destination has already been registered.",
            "Code": "InvalidInputFatalException"
        }
    }
}
```



#### Unsubscribe from notification queue

*request*

```
DELETE /api/v1/hyperion/subscribe
```

body:

```json
{
  "queue_url": "https://sqs.us-west-2.amazonaws.com/631158685056/tst"
}
```

*response*

```json
{
    "DeregisterDestinationResponse": {
        "ResponseMetadata": {
            "RequestId": "dabf1af8-7921-4003-88c7-88ce0faae7a4"
        },
        "DeregisterDestinationResult": {}
    }
}
```

If already unsubscribed error will return:

```json
{
    "ErrorResponse": {
        "RequestId": "8d87c908-bd0f-491d-bd7c-fd4b781c0613",
        "Error": {
            "Type": "Sender",
            "Message": "An exception was thrown while attempting to access the Destination. This can happen if the Destination has not been registered.",
            "Code": "InvalidInputFatalException"
        }
    }
}
```

#### Get all available object_schemas

*request*

```
GET /api/v1/hyperion/object_schema
```

*response*

```json
{
    "items": [
        {
            "name": "amazon_clothing",
            "id": 6
        }
    ],
    "count": 1
}
```

#### Get object_schema by its name

*request*

```
GET /api/v1/hyperion/object_schema/:schema_name
```

```json
{
    "schema_name": "amazon_clothing",
    "schema": {
        "type": "object",
        "title": "amazon_clothes_product",
        "properties": {
            "attributes": {
                "type": "object",
                "required": [
                    "code",
                    "brand",
                    "bulletPoint1",
                    "bulletPoint2",
                    "bulletPoint3",
                    "bulletPoint4",
                    "retailPrice"
                ],
                "properties": {
                    "upc": {
                        "type": "string",
                        "title": "UPC"
                    },
                    "title": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "taxCode": {
                        "type": [
                            "string",
                            "A_GEN_NOTAX"
                        ]
                    },
                    "retailPrice": {
                        "widget": "price",
                        "type": "object",
                        "properties": {
                            "value": {
                                "type": "number",
                                "minimum": 0,
                                "default": 0
                            },
                            "currency": {
                                "type": "string",
                                "minLength": 3,
                                "maxLength": 3
                            }
                        }
                    },
                    "manufacturer": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "itemType": {
                        "type": [
                            "hidden",
                            "null"
                        ]
                    },
                    "description": {
                        "widget": "richText",
                        "type": "string"
                    },
                    "department": {
                        "type": [
                            "hidden",
                            "null"
                        ]
                    },
                    "code": {
                        "type": "string",
                        "title": "SKU",
                        "minLength": 1
                    },
                    "category": {
                        "type": [
                            "hidden",
                            "clothes"
                        ]
                    },
                    "bulletPoint4": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "bulletPoint3": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "bulletPoint2": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "bulletPoint1": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "brand": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "activeTo": {
                        "type": [
                            "string",
                            "null"
                        ],
                        "format": "date-time"
                    },
                    "activeFrom": {
                        "type": [
                            "string",
                            "null"
                        ],
                        "format": "date-time"
                    }
                }
            }
        },
        "$schema": "http://json-schema.org/draft-04/schema#"
    },
    "id": 6
}
```

#### Get schema by category id

*request*

```
GET /api/v1/hyperion/object_schema/category/7132434011
```

*response*

```json
{
    "schema_name": "amazon_clothing",
    "schema": {
        "type": "object",
        "title": "amazon_clothes_product",
        "properties": {
            "attributes": {
                "type": "object",
                "required": [
                    "code",
                    "brand",
                    "bulletPoint1",
                    "bulletPoint2",
                    "bulletPoint3",
                    "bulletPoint4",
                    "retailPrice"
                ],
                "properties": {
                    "upc": {
                        "type": "string",
                        "title": "UPC"
                    },
                    "title": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "taxCode": {
                        "type": [
                            "string",
                            "A_GEN_NOTAX"
                        ]
                    },
                    "retailPrice": {
                        "widget": "price",
                        "type": "object",
                        "properties": {
                            "value": {
                                "type": "number",
                                "minimum": 0,
                                "default": 0
                            },
                            "currency": {
                                "type": "string",
                                "minLength": 3,
                                "maxLength": 3
                            }
                        }
                    },
                    "manufacturer": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "itemType": {
                        "type": [
                            "hidden",
                            "null"
                        ]
                    },
                    "description": {
                        "widget": "richText",
                        "type": "string"
                    },
                    "department": {
                        "type": [
                            "hidden",
                            "null"
                        ]
                    },
                    "code": {
                        "type": "string",
                        "title": "SKU",
                        "minLength": 1
                    },
                    "category": {
                        "type": [
                            "hidden",
                            "clothes"
                        ]
                    },
                    "bulletPoint4": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "bulletPoint3": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "bulletPoint2": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "bulletPoint1": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "brand": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "activeTo": {
                        "type": [
                            "string",
                            "null"
                        ],
                        "format": "date-time"
                    },
                    "activeFrom": {
                        "type": [
                            "string",
                            "null"
                        ],
                        "format": "date-time"
                    }
                }
            }
        },
        "$schema": "http://json-schema.org/draft-04/schema#"
    },
    "id": 6
}
```
