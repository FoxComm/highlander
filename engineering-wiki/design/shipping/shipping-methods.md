# Shipping Methods

This document provides an overview for how shipping methods work in FoxCommerce,
and specifically with how they interact with the warehouse and inventory
management system (_middlewarehouse_) and the order management system
(_phoenix_).

Broadly speaking, we can divide shipping methods into four parts:

1. **Service Description:** The traits of the shipping service that the
   customer should expect to receive. Examples include estimated time to
   arrival or method (ground vs air).

2. **Pricing:** There are two available types of pricing: flat rate and
   variable. Flat rate is a fixed price set by a merchant, while variable will
   be determined by the carrier (e.g. UPS, FedEx, USPS).

3. **Shipping Rules:** Whether or not a shipping method is applicable to a given
   order or shipment.

4. **Order Shipping Method:** A shipping method when applied to an order.

## Service Description

The Service Description is the root object for a shipping method that details
information such as the ETA to display to the customer, the description to show
on the storefront, and information about which carrier and external shipping
option will be used.
