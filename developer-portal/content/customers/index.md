# Customers

The Fox Platform has a model for customers and guest accounts. 

## Quickstart
--- row

This quickstart will introduce you to accounts and the checkout process.
The end will have more articles detailing specifics and references to the API.

---

--- row

<<< left
### Signup
A Customer can either be a guest or have a login with a password. In terms of buying
products there are several advantages for customers having a login such as having access
to their address book and payment methods.
<<<

<img class='eimg' src="data/login.png"/>

>>> right

### Signup

Signup will register a user and return the user information and a JWT authorization token.

``` javascript
    var fox = new FoxApi();
    fox.auth.signup('john@doe.com', 'John Doe', 'password')
        .then(({jwt, customer}) => {
            fox.addAuth(jwt);
            //do stuff with customer
        });
```

### Guest

Guest accounts are automatically created if a user isn't logged in and touched
their cart. During checkout you want to set the users email.

``` javascript
    var fox = new FoxApi();
    fox.account.update({'foo@bar.com'});
```

>>>

---

--- row

<<< left
### Addresses
Accounts can have an address book which can have any number of addresses.
These addresses can be used for billing and shipping information. Setting a
default address is required for single click checkout.

<img class='eimg' src="data/addresses.png"/>
<<<

>>> right

### Getting Addresses

``` javascript
    var fox = new FoxApi();
    fox.auth.login('john@doe.com', 'password', 'org')
        .then(({jwt, user}) => {
            fox.addAuth(jwt);
            fox.addresses.list().then( (addresses) => {
                //do something with addresses such as choosing one during checkout.
            });
    });
```

>>>

---

--- row

<<< left
### Wallet
Accounts can also have a wallet which stores the customers credit card information.
Setting a default credit card is required for single click checkout.

<img class='eimg' src="data/wallet.png"/>
<<<

>>> right
### Fetching Cards

``` javascript
    var fox = new FoxApi();
    fox.auth.login('john@doe.com', 'password', 'org')
        .then(({jwt, user}) => {
            fox.addAuth(jwt);
            fox.creditCards.list().then( (cards) => {
                //do something with cards set as picking one during checkout.
            });
    });
```
>>>

---

--- row

<<< left
### Cart

A customer cart is stored server side so that they can access it from any device.
There is one cart per customer per channel. The cart is designed to support diverse
checkout flows and there is no particular order in preparing the cart for checkout.

<img class='eimg' src="data/cart.png"/>
<<<

>>> right

### Getting The Cart
``` javascript
    var fox = new FoxApi();
    fox.auth.login('john@doe.com', 'password', 'org')
        .then(({jwt, user}) => {
            fox.addAuth(jwt);
            fox.cart.get().then( (cart) => {
                //do something with the cart like render it.
            });
    });
```

### Adding Products
``` javascript
    var fox = new FoxApi();
    fox.auth.login('john@doe.com', 'password', 'org')
        .then(({jwt, user}) => {
            fox.addAuth(jwt);
            fox.cart.addSku('SKU-123', 10);
    });
```

>>>

---

--- row

<<< left
### Checkout

Once a cart is in a good state, you can checkout to create an order.
The cart has a validator which returns errors about what information is missing
or incorrect to create an order

<img class='eimg' src="data/cartvalidation.png"/>

Checkout requires...
  - An email address
  - A Payment Method
  - A Shipping Address
  - A Shipping Method

Optionally you can apply coupons to discount line items, shipping, or off the total order.

<img class='eimg' src="data/checkout.png"/>

These items can be provided in any order which allows diverse checkout flows.
<<<

>>> right

### Adding a New Shipping Address
``` javascript
    var fox = new FoxApi();
    fox.auth.login('john@doe.com', 'password', 'org')
        .then(({jwt, user}) => {
            fox.addAuth(jwt);

            var address = {
                name: "John Doe",
                address1: "325 W Richmor",
                address2: "",
                city: "Seattle",
                phoneNumber: "6666666666",
                zip: 98109,
                regionId: regions["WA"],
                isDefault: false,
                country: 'United States'
            };

            fox.addresses.add(address).then((newAddress) => {
                fox.cart.setShippingAddressById(newAddress.id);
            });
    });
```

### Adding a Credit Card
``` javascript
    var fox = new FoxApi();
    fox.auth.login('john@doe.com', 'password', 'org')
        .then(({jwt, user}) => {
            fox.addAuth(jwt);

            var stripe = Stripe('pk_test_6pRNASCoBOKtIshFeQd4XMUh');
            stripe.tokens.create({
                    card: {
                    "name": "John Doe",
                    "number": '4242424242424242',
                    "exp_month": 12,
                    "exp_year": 2018,
                    "cvc": '123'
                    }
            }).then((token) => {;
                fox.creditCards.createCardFromStripeToken(token, address)
                    .then((card) => {;
                        fox.cart.addCreditCard(card.id);
                    });
            });

        });
```

>>>

---

--- row

## Learn More

--- 

--- row 

<<< left
### Guests

Anyone new who visits your storefront can have a guest account. Guest accounts
can have server side carts and can order products without having to sign up.
<<<

>>> right
[Create a guest account with an email](guest.md)
>>>

---

--- row

<<< left
### Accounts

Customers can have a login with a password which allows them to access their address
book, credit cards, and other information.
<<

>>> right
[Register a customer with an email and password.](account.md)
>>>

---

--- row

<<< left
### Login via Google

The Fox Platform supports registering and authenticating users using Google Accounts.
<<<

>>> right
[Oauth with Google Works](google.md)
>>>

--- row 

<<< left
### Address Book

Customers can have an address book with many addresses. A single address can 
be designated as the default one.
<<<

>>> right
[Managing the Address Book](address.md)
>>> 

---

--- row

<<< left
### Wallet

Customers can have many credit cards on file. A single card can be designated as 
the default one.
<<<

>>> right
[Managing the Wallet](wallet.md)
>>>

---
