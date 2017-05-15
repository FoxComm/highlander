--- row

<<< left
# Inventory Guide

This guide will help you understand the Inventory system and how it functions during
the checkout process.

The Fox Platform allows you to manage individual inventory items across warehouses. 
Inventory is automatically integrated with the Order Management System. Integration
with a 3rd Party WMS is handled by integrating with the Event System and using
the Inventory APIs.

<<<

>>> right
<!-- include(../api-ref-snippet.md) -->
>>>

---

--- row

<<< left

## Stock Keeping Units (SKUs)

The Fox Platform allows you to define the attributes of the goods you are selling
including price, size, landing cost independent of inventory.

### More about SKUs
::: note
[Add and Remove SKUs](skus.html)
:::

<<<

---

--- row

<<< left

## Stock Items

The Physical item in a location is known as a Stock Item. The Fox Platform keeps track 
of each individual item with it's own state allowing a more flexible and accurate 
system for tracking your inventory. The inventory is not simply a number on the SKU.

### More about Stock Items
::: note
[Add and Remove Inventory](items.html)
:::

<<<

---

--- row

<<< left

## Stock Locations

The Fox Platform can manage Stock Items across locations, whether it is separate warehouses
or locations within a single warehouse.

### More about Locations
::: note
[Manage Locations](locations.html)
:::

<<<

---

--- row

<<< left

## Shipping Methods

You can manage your shipping methods and providers using the inventory system.
Shipping Methods can have arbitrary complex rules which allow for better display
and selection in the storefront.

### More about Shipping Methods
::: note
[Creating Shipping Methods](methods.html)
:::
::: note
[Add Rules to your Shipping Methods](rules.html)
:::
::: note
[Manage Shipping Providers](providers.html)
:::
<<<

---

--- row

<<< left

## Checkout Process

Inventory is checked during the checkout process and prevents checkout if the 
Available for Sale is less then the quantity requested. During checkout inventory
goes into a hold state before being reserved. The order goes through a Remorse Hold
state before inventory is reserved, allowing a window of time for customers to cancel
orders inventory being fulfilled.

The inventory system keeps track of the time each individual item enters the warehouse.
This allows for specifying which inventory is reserved during checkout by date.
This is especially useful if you sell perishable goods.

### More about Checkout
::: note
[Checkout and Inventory](checkout.html)
:::
<<<

---

<!-- include(../support.md) -->
