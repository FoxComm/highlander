#### March 4, 2015  

## Agenda  
- New team member welcome  
- Review work over the past week  
- Mini-roadmap walkthrough
- Process overview    
- Plan for next cycle  
- Availability  

## New team member welcome  

I think we've all met Nicholas by now, but welcome to the team! Nick's position is Principle Software Engineer, and he'll be helping drive the architecture and implementation for the backend service.  

## Review work over the past week  

**Emmanuel**  

...

**Herman**  

...

**Jacques**

...

**Nick**  

...

**Jeff**  

...  

## Mini-roadmap  

_The full roadmap isn't complete, but lives here:_ [https://github.com/FoxComm/foxcomm2.0/wiki/Product-Roadmap](https://github.com/FoxComm/foxcomm2.0/wiki/Product-Roadmap)  

### FoxComm Goals  

- Solve increasing complexity with SMBs  
- Help customers use business model innovation to scale  

### Customer Archetype for Beta/1.0  

- Yearly revenue of $5-25M USD (this is what we are terming the SMB market)
- Operates in the fashion or beauty industry
- Has (or desires to have) a small technology organization
- Is located within the United States, likely New York, Los Angeles, or San Francisco
- Has ambitions it implement business model innovation around revenue growth (e.g., subscription/replenishment services, or loyalty)

### What is Beta?  

The goal for Beta is to ship what we're calling **core commerce**. Think of this as being a bit better than parity compared to something like Magento or Spree commerce. Alternatively, the full functionality of BeautyKind is a good standard. Here's how I think that breaks down:  

#### API  

##### Product Information Management (PIM)  
- Product CRUD  
	- Define standard and custom attributes
- Tagging products  
- Region and Modality are considered in the data model, but it is not fully exposed to the API/UI  
- Pricing rules  
	- Manual  
	- Exchange rate + tariff + tax based conversions
- Basic support for return rules
	- Can an item be returned?  
	- What is the return window?  
	- How much is refunded to the customer?  
- Support for shipping rules  
	- Rules are defined in Inventory and can be associated with products  
	- Example: ORM-D  
- Support payment rules
	- Due at checkout
	- Due at shipment  
	- Invoice  
- Reviews CRUD  

##### Merchandising
- Product Details Page CRUD
- Taxonomy CRUD  
- Collections CRUD  
- Cross-Sells CRUD  
- Simple Promotions CRUD  
	- Percentage-based discounts  
	- $x-off discounts  

##### Inventory/Operations Management  
- Stock location CRUD  
- Inventory CRUD  
- Stock transfers  
- Shipping (multi-route)  
- View of overall inventory  
- Support for returns  
- 3PL integration (likely Newgistics as the first trial)  

##### Transactions
- Cart  
- Order  
- Recurring transactions (i.e., subscriptions)  

##### Payment  
- Process payments through Stripe and Braintree  
- Handle refunds  

##### Users  
- User registration  
- User authorization  
- Fine-grained permissions  

##### Messaging  
- Integration to ESP  

#### UI
- API data access layer    
- Admin UI  

### Key Dates

- _April 15, 2015_: Beta midpoint check in
- _July 1, 2015_: start accepting customers for Beta
	- The FoxComm team will guide 1-2 Beta customers through the process of migrating
- _September 15, 2015_: Beta custome #1 goes live
- _February 1, 2016_: ship FoxComm 1.0  

### Approach to Building Beta  

Our goal is to be able to accept a Beta customer in Q3 and to do that we will need to make a number of tradeoffs and priorities. Here is the suggested priority:  
  
**Work (mostly) backwards through the checkout flow**  

1. Transactions  
2. Inventory/Order Management  
3. Payments  
4. Users  
5. PIM  
6. Merchandising  

The reasoning for this order:  

- We're solving the biggest pain points from Spree/Magento first  
- We believe that Spree can be split into two components: 1.) users, product catalog, and promotions; and 2.) transactions, inventory, and payment. If we can't get to all **core commerce** by Q3, we may consider ripping out part of Spree, rather than the whole thing.  

_Thoughts?_

## Process overview  

This is the proposed process that we should decide upon today:  

**Methodology**  

Use the _post estimation methodology_ recommended by Nick. The process is as follows:  

- Do not assign points ahead of time  
- Run weekly iterations (sprints/cycles)  
- Hold a weekly retrospective and do the following with all the completed tickets:  
	- Assign a weight to each ticket: trivial, medium, significant  
	- Record with developer completed the item  
	- Record a journal entry that records velocity based on the number of completed trivial, medium, and significant tasks  

**Tools**  

- Bug tracking: GitHub Issues  
- Project management: Trello
	- Has just three lanes: Incoming, In Progress, Done
	- Board: [Product Backlog](https://trello.com/b/ss6mxmlA/product-backlog)  

**Open Questions**  

- What should our retrospective schedule be?  
- When do we want to consider having demos?  

## Plan for next cycle  

- Emmanuel  
- Herman  
- Jacques  
- Nick  
- Jeff  

## Availability  

- Emmanuel  
- Herman  
- Jacques  
- Nick  
- Jeff  

