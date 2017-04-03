# Consumers

These are consumers processing kafka topics related to the intelligence commerce 
system. The consumers are outlined below.

| Project                                | Description                                                                                                  |
|:---------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| [digger-sphex](digger-sphex)           | The digger sphex is a consumer that processes nginx logs and aggregates data into henhouse |
| [orders-sphex](orders-sphex)           | The orders consumer parses `order_checkout_completed` activities and aggregates data into henhousee |
| [orders-anthill](orders-anthill) | The orders consumer parses `order_checkout_completed` activities and aggregates data into anthill |

