# Marketplace Functionality Design Document

**Author:** Adil Wali  
**Date:** August 6th, 2016

## Overview
The Fox platform strives to provide powerful features that support _business model innovation_ (BMI) 
in addition to supporting operations and growth.  Our first area of focus for BMI is supporting marketplace 
functionality. This decision is based on the current interest expressed in the platform that we've learned of 
through customer development and research.

Before we dive into the architecture around marketplace, we must first establish a shared understanding of what 
*marketplace* means.  Stated simply: _marketplace functionality is the enablement of third-party sellers to sell 
on your storefront._  

There are many types of marketplaces.  As it pertains to our design thinking here, there are two primary 
manifestation types that we are concerned with: 
 - **Unmanaged Marketplace:** The merchants are in control of the customer experience for their products.
  - Typically, the _merchant of record_ is the third-party seller; not the storefront.
  - The customer care is handled by the third-party seller, and usually facilitated by the storefront.
  - Examples includes: eBay, Etsy, Storenvy
 - **Managed Marketplace:** The storefront staff are in control of the customer experience and the way product is 
 presented to customers.
  - Typically, the merchant of record is the storefront in this case.
  - The customer care is handled by the storefront.  The customer feels as though they purchased directly from the storefront.
  In some cases, the customer may not even know that the product was sold by a third-party seller.
  - If there is shared control between the merchant and the storefront staff, we will refer to these still as managed marketplaces.
  (There are varying levels of management.)
  - Examples include: TrueFacet, Beepi, Real Real
  
The challenge with creating a robust set of marketplace functionality is in serving the diversity of use cases and manifestations of
marketplaces.  For a more in-depth overview of marketplaces and their diversity, see the 
[Marketplace Handbook](marketplace_handbook.pdf). 
  
## Design Goals and High-Level Requirements

Designing a robust marketplace system will be an iterative and collaborative process with our customers.  The marketplace landscape is not only vast today, it is ever-expanding.  Our goals today are centered around creating a robust marketplace sub-system that is generalizable, but realtively simple.  In other words, we do not want our desire to support many potential use cases to come at the expense of significant complexity in the system.

Some of the key notions of diversity that we want to support are as follows: 
 - **Merchant Control:** Merchants should have access to various elements of administrative functionality.  
  - **Multiple Accounts:** Merchants should be able to have multiple members of staff access the system and manage information.
  - **Flexible Privileges:** Enabling merchants to have admin accounts that can have flexibly scaled-back privileges.  
   - Ex. Store1 might allow merchants to only upload products.  Store2 might allow merchants to manage orders, products, and customers.
 - **Fulfillment Strategies:** Supporting different fulfillment strategies for different merchants. 
  - Ex. Merchant1 might be dropshipping orders based on Fox connectivity with their ERP.  Merchant2 might send the product to the Storefront's distribution center to hold as consignment inventory.  Merchant3 might dropship orders based on Fox connectivity directly with their OMS. 
 - **Comingling of Marketplace and Classic Commerce:** Much like Amazon.com and Walmart.com, Fox should support the notion of regular DTC commerce alongside 3rd-party sellers.   
 
Depending on the access-level that a merchant has within the marketplace, it might be able to access one or more of the following administrative features: 
 - **Order Management:** A merchant that is taking on both customer service and fulfillment may be able to access the orders that include their specific products. 
 - **Customer Care:** A merchant that is taking on customer service may be able to access customer information and customer communication tools. 
 - **Product Management:** Most merchants will have the ability to create, update, and manage product information around their specific catalog.

The Marketplace functionality is robust and very large in scope.  As such, there are some key features that we may want to support today, and other features for which we simply want to lay a robust foundation for future implementation.  Those features include: 
 - **Meta-merchandising:** The notion that the Storefront team can manage a layer of merchandising atop whatever the various marketplace merchants have produced.  Examples could be home page, primary site navigation, etc. 
 - **Workflow Management:** Depending on how managed the marketplace is, the Storefront staff may be managing a workflow of product launch.  This could include editing products before they go live or simply approving/rejecting them.

## Key Challenges and Areas of Complexity Around Marketplace

Because of the diversity of possible use cases for marketplace functionality, there are some particularly challenging sets of features and functionality that must be concerned.  

One of the key challenges with the rapid change and evolution in the commerce landscape is working to establish clear criteria on certain modalities.  With so much change afoot, the following question becomes rather difficult to answer: _when is a site a marketplace versus a traditional retailer?_ 

Let's dive into some of the underpinning notions to explore this further:

 - **Merchant-of-Record:** Merchant-of-record, or MOR, is the ultimately responsible party for making the sale.  The MOR is typically responsible for engaging in customer care as well as paying all tax liabilities to appropriate jurisdictions.  
  - The classic definition of a marketplace was when the merchant (and not the marketplace itself) was the MOR.  This is evolving.
  - If for example, the customer never receives the product, the MOR is th one who is responsible for re-shipping it.
  - Our belief is that mixed-modality MOR is a very edge-case scenario.  As such, our goal is to be able to make this a configurable setting at the storefront level (not the per-merchant level).  
 - **Holding Inventory:** One of the line-blurring elements of a marketplace is the occurrence of the the marketplace storefront entity actually holding inventory on behalf of a merchant.  This happens in the case of consigned inventory, or in the case of the marketplace also providing outsourced fulfillment services (such as Fulfilled By Amazon). 

**Consigned inventory** can be confusing because the definition of a marketplace to many people is "a site that sells other merchant's goods."  Some storefronts that consider themselves traditional retailers will sell consigned inventory, while others that consider themselves marketplaces.

The advent of managed marketplaces combined with consigned inventory furthers the blurriness around when a retailer is truly considered to be a marketplace.  The truth is: *the lines are blurring between traditional retail and various forms of BMI.*  As such, we should not consider the distinction between marketplace and traditional retail to be binary.  Instead, we should think of marketplace functionality as a continuum, with some retailers pushing more into that realm than others.  

In today's world, everyone can adopt some element of marketplace functionality.  And FoxCommerce's mission, in this respect, is to help people push those innovative boundaries of customer experiences.  

For the sake of clarity, FoxCommerce considers the category of marketplace functionality to include anything that _enables a retailer to sell third-party goods to their customers without taking on significant additional risk._   

## Some Specific Scenarios to Consider

Sometimes the best way to deal with a fast-evolving world without clear dividing lines is to examine specific scenarios that describe functionality we may wan to support. 

 - **Both Vendors and Merchants:** Based on the distinction above, we think of the merchant as the _party who has the **title** of the goods when they are sold._  This denotes the party with the ultimate responsibility for the sale.  Vendors, on the other hand, are folks that a retailer buys/obtains the product from.  Much of their functionality is similar.
 - **Multi-Merchant SKUs:** In some marketplace cases such as Amazon, multiple merchants can carry the same SKU.  In this case, the PDP can show multiple options for purchase of the same product. 
  - This also creates the potential for a reverse-auction dynamic whereby the lowest-priced merchant actually is the default merchant shown.
 - **Standalone Merchant Sites:** In certain marketplaces like Etsy and Storenvy, merchants are able to have their products be accessible through a standalone site on a different domain (such as www.merchant.com) in addition to being generally available in the marketplace.
 - **Content Management:** In some marketplace cases, the merchants not only have control over the product information, but also to other content throughout the site.  This could include content and styling on category pages, their homepage (if they have a standalone site), and even new custom content pages.
 - **Merchant Metadata:** In a classic marketplace case (and even in some managed marketplaces), certain content can be surfaced about the merchant.  This could include ratings and reviews, a content description with imagery, etc.  
 - **Order Splitting:** While we mentioned above that merchants should have access to some element of order data, we have not actually covered the method by which this happens.  Some of the key considerations are: making it easy for merchants to see , manage, fulfill, and accept returns against their orders.  The ways that come to mind for how we might accomplish this are as follows:
  - *Privelege-Specific access to orders:* Merchants can access any order record that contains a product that they sell on the marketplace.  Their scope of possible edits is limited to the line-items that they sell.  
  - *Order-splitting based on merchant:* Splitting an order into N sub-orders where N is the number of merchants that the customer purchased from.  In this way, each order is scoped to a merchant, so each merchant can have full control of managing these orders.  
  - Irrespective of the path we choose, there will be non-zero complexity in managing payment, refunding, and customer care for each merchant. 
 
## Build What we Need Today; Lay a Foundation for Tomorrow

There is quite a bit to consider above in the way of scope and future architecture.  This leaves us with the question today around exactly what to focus on first, and how to ensure we have a robust and scalable data model and architecture for the future.  

The plan, for now, is as follows:
 - Start with a basic data model for `Merchants`.  
  - Basic merchant information such as `name` and `desc`. 
  - Merchant locations for the purposes of tax nexus. 
 - Enable merchants to have more than one account.
  - Extend `IdentityKind`
  - Ideally allow for google apps auth here as well
 - Create notion of Vendor (This is basically a merchant that does not have responsiblity for customer care or how the product is presented)
  - Vendors can have many vendor contacts.  These are the people the retailer might have relationships with; they may or may not have accounts.
 - Map products to vendors and merchants: 
  - Context approach
  - M2M mapping between SKUs and Vendors
  - SKU can be owned by merchant.
  - A merchant can have vendors also.
 - Enable merchant account to see scoped:
  - Products
  - Orders
 - Begin tracking baseline metrics by both vendor and merchant.
  - Conversion rate
  - Return rate
  - Average time to ship
  - Average time to arrive
 - Build `MerchantConfiguration` data model.  Include:
  - Fulfillment options of ERP, WMS, eComm platform.
  - Inventory management to include Fox or 3rd party.
 - Enable product catalog on storefront to be scoped by vendor.   
  
  
Punted to the medium-term, so we should still passively think about these:
 - Enable merchants to have configurable privileges. 
 - Investigate Splitting orders
 - 
Punted to the long-term, so we should consider these less (but, ideally, some):
 -  True vendor management - including purchase order management and demand planning.
 -  Configurable pre-order and back-order by merchant. 
 -  Go deeper into public-facing content/metadata by vendor

## High-Level Systems Thinking

So we are now left with the key question: what parts of the system should live where?  Here are some initial thoughts to that end:

 - Vendor Management can potentially be its own service; but it might make sense to wait on that, since we will have a notion of vendorAccounts.  
  - _Today: Phoenix_
 - StoreConfiguration could also be a separate service. 
  - _Today: Open_
 - Vendor/Merchant Configuration could be a separate service.
  - _Today: Open_
 - Analytics and insights functionality for merchants/vendors should _live in HenHouse._
 - The key functionality of merchants/vendors accesses and updating information about their products and orders should _live in Phoenix._

Our mental model for what systems live where is as follows: *let's minimize complexity until all of the key functionality emerges.*  In other words, we can err on the side of keeping things in Phoenix until we fully understand how easily-decoupled the Vendor/Merchant functionality is.

## Going Deeper into Merchant Configuration

 - **Inventory:** We assume that Fox will have a local cache of merchant inventory to keep the site fast and error-free.  We will poll that inventory from a merchant's systems **or** that merchant will be responsible to call our inventory endpoints and update in real-time.  
  - We may later roll plugins for certain systems such as NetSuite and Magento to make it easy for those systems to update our inventory system in real-time.
