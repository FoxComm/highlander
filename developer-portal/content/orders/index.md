--- row

<<< left 

# Orders Quickstart

The Fox Platform has a fully function order management system. Customers create carts
which get converted to orders during checkout.

## Line Items

Line Item are products the customer wants to purchase. Each line item has a reference and
optional metadata. If a promotion is applied to a cart, line items may also have matching
adjustments.

### More about Line Items
::: note
[Reading Line Items From an Order](line-items.html)
:::

## Payment Methods

There is support for several payment methods including credit cards, store credit, and
gift cards.

### More about Payment Methods
::: note
[Getting the Payment Method for an Order](payment-methods.html)
:::


## Billing and Shipping Addresses

A cart isn't valid until it has a billing and shipping address. These addresses
may be retrieved from a customers address book.

### More about Addresses
::: note
[Reading the Billing and Shipping Address From an Order](addresses.html)
:::

## Orders

Orders are created after a cart goes through the checkout operation. Orders are
read only except for their state which can go through several transitions.

### More about Orders
::: note
[Understanding the order States](states.html)
:::

<<<

>>> right
<!-- include(../api-ref-snippet.md) -->
>>>

---

<!-- include(../support.md) -->
