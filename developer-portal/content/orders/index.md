# Orders

The Fox Platform has a fully function order management system. Customers create carts
which get converted to orders during checkout.

## Quickstart

## Learn More

### Carts

Carts are composed of line items, billing and shipping addresses, and payment methods.
If a cart is in a valid state then it can be converted to an order with the checkout operation.

- [Create and manage a Cart](carts.md)

### Line Items

Line Item are products the customer wants to purchase. Each line item has a reference and
optional metadata. If a promotion is applied to a cart, line items may also have matching
adjustments.

- [Add line items to a Cart](line-items.md)

### Payment Methods

There is support for several payment methods including credit cards, store credit, and
gift cards.

- [Add a payment method to a Cart](payment-methods.md)

### Billing and Shipping Addresses

A cart isn't valid until it has a billing and shipping address. These addresses
may be retrieved from a customers address book.

- [Add a billing and shipping address to a Cart](addresses.md)

### Orders

Orders are created after a cart goes through the checkout operation. Orders are
read only except for their state which can go through several transitions.

- [The checkout process](checkout.md)








