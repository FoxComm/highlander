---
id: hello-world
title: Hello World
permalink: docs/hello-world.html
prev: installation.html
next: introducing-jsx.html
redirect_from:
  - "docs/index.html"
  - "docs/getting-started.html"
---

The easiest way to get started with Fox is to use our starter storefront and core admin.  In order to use them, you will need a provisioned environment.  If you do not have one, please contact us at [support@foxcommerce.com](mailto:support@foxcommerce.com).

You do not need to install anything to get started.  You can begin by issuing basic CURL commands to the API:
The smallest React example looks like this:

```sh
curl --request GET \
  --url https://api.foxcommerce.com/v1/products/default/1 \
  --header 'accept: application/json' \
  --header 'content-type: application/json'
```

You will receive a list of our default seeded products!  


## Where are you coming from?

Most Fox users are migrating from a legacy platform such as Magento or BigCommerce.  If this is your case, then you should get started by evaluating one of the migration guides below.  

 * [Migrating from Magento](migrating-from-magento.html) 
 * Migrating from BigCommerce
 * Migrating from Shopify
 * Migrating from IBM Websphere
 * Migrating from Oracle ATG


Do not hesitate to contact us as you ramp up on the capabilities of the Fox platform. 
