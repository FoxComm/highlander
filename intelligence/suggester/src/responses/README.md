# Responses

GoLang structs for various API responses

## Ant Hill Prouduct Response Example

```
{
  "taxonomies": {},
    "slug": "duckling",
    "id": 5,
    "productId": 17,
    "skus": [
      "SKU-ZYA"
    ],
    "title": "Duckling",
    "context": "default",
    "scope": "1.2",
    "externalId": null,
    "activeTo": null,
    "archivedAt": null,
    "tags": [
      "sunglasses",
      "readers"
    ],
    "description": "A fit for a smaller face.",
    "albums": [
      {
        "name": "Duckling",
        "images": [
        {
          "alt": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg",
          "baseurl": null,
          "title": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg",
          "src": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg"
        }
        ]
      }
    ],
    "activeFrom": "2017-03-01T21:19:08.333Z"
}
```

## Twilio API SMS Messages Response Example

```
{
  "sid": "MMc781610ec0b3400c9e0cab8e757da937",
    "date_created": "Mon, 19 Oct 2015 07:07:03 +0000",
    "date_updated": "Mon, 19 Oct 2015 07:07:03 +0000",
    "date_sent": null,
    "account_sid": "AC771813582c88da5d48a1844ef4fb5f28",
    "to": "+15558675309",
    "from": "+15017250604",
    "body": "This is the ship that made the Kessel Run in fourteen parsecs?",
    "status": "queued",
    "num_segments": "1",
    "num_media": "1",
    "direction": "outbound-api",
    "api_version": "2010-04-01",
    "price": null,
    "price_unit": "USD",
    "error_code": null,
    "error_message": null,
    "uri": "/2010-04-01/Accounts/AC771813582c88da5d48a1844ef4fb5f28/Messages/MMc781610ec0b3400c9e0cab8e757da937.json",
    "subresource_uris": {
      "media": "/2010-04-01/Accounts/AC771813582c88da5d48a1844ef4fb5f28/Messages/MMc781610ec0b3400c9e0cab8e757da937/Media.json"
    }
}
```
