#Services

Eggcrate is broken up into logical "services". Each service implements one or more 
endpoints. 

| METHOD | REQUEST                       | QUERY PARAMS | RESP                                                                                               |
|:----|:---------------------------------|:-----------------------------|:---------------------------------------------------------------------------------------------------|
| GET | `/ping`                          |                              | pong |
| GET | `/productFunnel`                 | `?from=<number>&to=<number>` | `{"SearchViews":<number>,"PdpViews":<number>,"CartClicks":<number>,"CheckoutClicks":<number>,"Purchases":<number>,"SearchToPdp":<number>,"PdpToCart":<number>,"CartToCheckout":<number>,"CheckoutPurchased":<number>}` |
| GET | `/productFunnel/:id`             | `?from=<number>&to=<number>` | `{"SearchViews":<number>,"PdpViews":<number>,"CartClicks":<number>,"CheckoutClicks":<number>,"Purchases":<number>,"SearchToPdp":<number>,"PdpToCart":<number>,"CartToCheckout":<number>,"CheckoutPurchased":<number>}`|
| GET | `/productSum/:page/`             | `?from=<number>&to=<number>` | `{"Step":<string>,"Sum":<number>}` |
| GET | `/productSum/:page/:id`          | `?from=<number>&to=<number>` | `{"Step":<string>,"Sum":<number>}` |
| GET | `/productStats/:channel/:id`     | `?from=<number>&to=<number>` | `{"TotalRevenue":<number>,"TotalOrders":<number>TotalPdPViews":<number>,"TotalInCarts":<number>,"ProductConversionRate":<number>,"Average":{"TotalRevenue":<number>,"TotalOrders":<number>,"TotalPdPViews":<number>,"TotalInCarts":<number>,"ProductConversionRate":<number>},"ActiveProducts":<number>}` |
