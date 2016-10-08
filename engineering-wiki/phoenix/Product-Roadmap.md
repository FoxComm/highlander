**This document is a work-in-progress: no one should take it as gospel.**  

## Overview  

The goal of this document is to store the high-level approach to the development of FoxComm 1.0. It will discuss the goals of the company, the customers to which we plan to sell, key dates, and what needs to be built.  

It should be considered a living document that will grow with the organization.  

## Relevant Documents

- [Engineering Manifesto](https://github.com/FoxComm/foxcomm2.0/wiki/Engineering-Manifesto) - an outline of the overall technical challenges and the guiding principles to use when solving them;
- [API Documentation](http://api.foxcommerce.com) - documentation about the current best thinking about our APIs and examples of how they may be used in the real world. See Jeff if you need the credentials to access the documentation. 

## Company Goals  

FoxCommerce is an eCommerce platform built for small and medium size businesses ranging between $1M and $15M in yearly revenue. It is designed to help eCommerce companies of varying types manage the increasing **complexity** of business that comes with scale and the need for **business model innovation** that we believe to be necessary for success in the future of commerce.  

### Problem #1: Complexity  

As companies grow past $1-3M in revenue, we start to see the businesses grow in complexity past what is currently supported by traditional open source companies, such as [Spree](http://www.spreecommerce.com) and [Magento](http://www.magento.com). This is because the data models for such platforms are tailored to Mom and Pop business and fail to handle things like: advanced details in the product model, sophisticated promotions, multiple distribution centers, and complex shipping logic.  

Our goal is to architect our data model and system to handle this complexity and author our APIs so that customers can sanely augment the capabilities of FoxCommerce, should the need arise.  

### Problem #2: Business Model Innovation  

Digital commerce is a now a much different beast than it was 5-10 years ago. Back then, a company could reasonably scale and find success by simply offering better products and a refined shopping experience. Successes by companies in the past have led to a much more mature ecosystem - now we believe that companies will scale as a result of finding new and different ways to improve their user acquisition cost, customer lifetime value, and unit economics. We're currently seeing this with a variety of approaches, such as subscription services, marketplaces, and selling across channels.  

## Customer Archetype 

The customer archetype for the Beta and 1.0 fits the following properties:  

- Yearly revenue of $5-25M USD (this is what we are terming the SMB market)  
- Operates in the fashion or beauty industry  
- Has (or desires to have) a small technology organization  
- Is located within the United States, likely New York, Los Angeles, or San Francisco  
- Has ambitions it implement business model innovation around revenue growth (e.g., subscription/replenishment services, or loyalty)  

## Key Dates  

- _April 15, 2015_: Beta midpoint check in
- _July 1, 2015_: start accepting customers for Beta  
    - The FoxComm team will guide 1-2 Beta customers through the process of migrating  
    - More details about Beta below  
- _September 15, 2015_: Beta custome #1 goes live  
- _February 1, 2016_: ship FoxComm 1.0

## Product Spheres  

The platform for 1.0 is broken down into a number of different spheres. These may expand in the future as FoxComm moves toward an enterprise market. Of note, these spheres do **not** need to correspond to exact implementations of a microservice, that is an implementation detail to be saved for later.  

### Product Information Management (PIM)  

The Product Information Management (PIM) sphere is the central nervous system of FoxCommerce and stores information describing everything from the content that should be shown on the website to rules for how to handle shipping, purchasing, and returns. In short, it can be thought of as the source of truth for all information about any product.  

**API Documentation:** [http://api.foxcommerce.com/pim](http://api.foxcommerce.com/pim)
 
### Inventory/Order Management  

Support for interacting with different stock locations, warehouses, inventory, shipping, and returns.  

**API Documentation:** N/A

### Merchandising  

The Merchandising module controls the creation and display of the product detail pages, tags, collections, taxonomies, cross-sells, and promotions. It is essentially a tool for store owners to arrange content in the best possible way for a customer in order to generate sales.  

**API Documentation:** [http://api.foxcommerce.com/merchandising](http://api.foxcommerce.com/merchandising).

### Transactions  

### Payments  

### User Management and Permissions  

### Accounts  

### Analytics  

### A/B Testing