#Hyperion wiki

Hyperion is a mecroservice for comunications with Amawon MWS:
    * submit products
    * getting orders
    * getting some product related info


How to start and some quick info is [here](https://github.com/FoxComm/highlander/tree/add_amazon_microservice/hyperion)

##Examples

All requests should have two headers:
* `jwt` header to work with Phoenix-scala
* `customer_id` to get Amazon credentials

####Get client credentials

*request*

```
GET /v1/credentials/:customer_id
```

*response*

```json
{
    "seller_id": "seller_id123",
    "mws_auth_token": "token1212",
    "client_id": 123
}
```

####Store cleint credentials

*request*

```
POST /v1/credentials
```
body:

```json
{
  "client_id": 222,
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

####Update client credentials

*request*

```
PUT /v1/credentials/:customer_id
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

####Submit product feed to MWS

*request*

```
POST /v1/products
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

####Submit products feed by ASIN

```
POST /v1/products/by_asin
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


####Submit price feed to MWS

*request*

```
POST /v1/prices
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


####Submit inventory feed to MWS

*request*

```
POST /v1/inventory
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

####Submit images feed

*request*

```
POST /v1/images
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

####List matching products by query string

*request*

```
GET /v1/products/search?q=:query_string
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

####Get feed submission result

_IMPORTANT:_ Feed can be processed with warnings. In most cases with warnings your product reached MWS.

*request*

```
GET /v1/submission_result/:feed_id
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

###Get matching product by ASIN

*request*

```
GET /v1/products/find_by_asin/:asin
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

####Get orders

_IMPORTANT:_ This endpoint will be upgraded soon. It will stay backward compatible but will have some additional params.

Params:

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
* last_updated_after * — DateTime is ISO8601 format `%Y-%m-%dT%H:%M:%SZ`
* buyer_email — The e-mail address of a buyer. Used to select only the orders that contain the specified e-mail address.
* seller_order_id — An order identifier that is specified by the seller. Not an Amazon order identifier. Used to select only the orders that match a seller-specified order identifier.

Params marked with * are mandatory.

####Get categories for ASIN

*request*

```
GET /v1/products/categories/:asin
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

####Search for Amazon category ID pair: `department — item-type`

|name|type|description|required?|
|----|----|-----------|---------|
|node_path |String|Query string |Yes|
|from |Integer|Used for paginate results. Default value is: `0` |No|
|size |Integer|Used for paginate results. Default value is: `10` |No|

*request*
```
GET /v1/categories?node_path=boys
```

**response**

```json
{
  "total": 4,
  "items": [
    {
      "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Baby/Baby Boys/Tops/T-Shirts",
      "node_id": 9057174011,
      "item_type": "novelty-infant-and-toddler-t-shirts",
      "department": "baby-boys"
    },
    {
      "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Boys/Tops & Tees/T-Shirts",
      "node_id": 9057094011,
      "item_type": "novelty-t-shirts",
      "department": "boys"
    },
    {
      "node_path": "Clothing, Shoes & Jewelry/Baby/Baby Boys/Clothing/Swim/Swimwear Sets/T-Shirt Sets",
      "node_id": 6259178011,
      "item_type": "infant-and-toddler-swimwear-t-shirt-sets",
      "department": "baby-boys"
    },
    {
      "node_path": "Clothing, Shoes & Jewelry/Boys/Clothing/Swim/Swimwear Sets/T-Shirt Sets",
      "node_id": 6259168011,
      "item_type": "swimwear-t-shirt-sets",
      "department": "boys"
    }
  ]
}
```

####Suggest categories by product title

*request*

```
GET /v1/categories/suggest?q=Spiderman T-Shirt
```

*response*

```json
{
    "secondary": [
        {
            "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Girls/Tops & Tees/T-Shirts",
            "node_id": 9057040011,
            "item_type": "novelty-t-shirts",
            "department": "girls"
        },
        {
            "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Women/Tops & Tees/T-Shirts",
            "node_id": 9056923011,
            "item_type": "novelty-t-shirts",
            "department": "womens"
        },
        {
            "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Boys/Tops & Tees/T-Shirts",
            "node_id": 9057094011,
            "item_type": "novelty-t-shirts",
            "department": "boys"
        },
        {
            "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Movie & TV Fan/Tops & Tees/T-Shirts",
            "node_id": 2491811011,
            "item_type": "movie-and-tv-fan-t-shirts",
            "department": null
        }
    ],
    "primary": {
        "node_path": "Clothing, Shoes & Jewelry/Boys/Clothing/Tops & Tees/Tees",
        "node_id": 1288961011,
        "item_type": "fashion-t-shirts",
        "department": "boys"
    },
    "count": 5
}
```

####Subscrube to notification queue

*request*

```
POST /v1/subscribe
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



####Unsubscribe from notification queue

*request*

```
DELETE /v1/subscribe
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
