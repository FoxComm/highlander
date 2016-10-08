The transactions component handles everything related to an order, it's separated in two parts, the first one manage every interaction a customer/company can do before placing an order, we call this the "cart". 

The second part is the order by itself, once the order is placed the cart becomes an order, here all interactions are supposed to be done by admin users.

## Cart features:

- Add products to the cart.
- Add delivery information.
- Add payment information.
- Redeem coupon codes.
- Calculate taxes according to each delivery information.

## Order features

- Create an order.
- Adjust an order.
- Update order state.
- Cancel an order.

### Add products to the cart

This is the beginning of every transaction, it happens when a user choose a product and decides to add it to its cart. When the product is added to the cart, the transactions component interacts with the following components:

- Inventory (Verifies the product is in stock)

### Add delivery information

This normally happens when the user is ready to pay for what is in his cart, here the user can decide if he wants to get everything delivered to the same address or split it into different shipments delivered to multiple addresses, for example one to the office and the other one to home, perhaps, he wants to pick one of the products in the closest physical store because it's available there.

In summary there are 4 types of deliveries from which the user can choose:
- Shipment
- Pick up in store
- Digital
- Service

Once the user has submitted the delivery information, the transactions component should offer him a set of delivery method options matching the submitted information, i.e. a 2 days shipping option (in the case of a shipment) or the closest physical store address (in the case of pick up in store).

Here we have interaction with the following components:

- Inventory (We ask the inventory system for delivery methods for the provided delivery information)

### Add Payment information

Following a similar approach to the delivery information, we allow the user to submit his payment information at anytime, we allow him to split the payment, so for example he's able to pay one part of the order with a gift card and the rest with a credit card.

The external components interactions are:

- Payments (we ask payments for the available payment methods for the user)

### Redeem coupon codes

It can happen a user have gotten a coupon code, so we offer him an option to redeem it, the user sends the coupon code and transactions validate it with the Merchandising component, if it's valid transactions applies a discount to the cart with the amount calculated by Merch.

Here, the external interactions are:

- Merchandising (validates and calculate the discount of a coupon code)

Blind Spots:

- Discount recalculations

### Tax calculation

Once we have delivery information associated to the cart, we can calculate taxes. The idea here is to call a third party (avalara, ...) the get the calculated tax amount for the order.

Blind Spots:

- Tax recalculations

### Create an order

The user is set, he added the products he want, he added his delivery information and also his payment information and perhaps he applied a coupon code, now it's time to place the order, several things happen here:

1. Stock validation.
2. Coupon codes validation. (do we need this double check??)
3. Prepare deliveries.
4. Set stock items on hold.
5. Authorize payments.
6. Create an order.

Here transactions interacts with the following components.

- Inventory (validates stock and prepares deliveries)
- Payments (Authorizes payments)
- Merchandising (Checks for coupon validity)

### Adjust an order

This task is an edge case, for example if the store takes a long time to fulfil the order, we might want to gratify the wait to the user with a discount, so  we allow an admin user to create an order adjustment.

### Update order state

After an order is placed, the order could have multiple states and an admin user should be able to transition the order, an order could have the following states (more TBD):

- Ready to deliver
- Delivered
- Cancelled
- Returned
- Partially returned

### Cancellations

TBD 

