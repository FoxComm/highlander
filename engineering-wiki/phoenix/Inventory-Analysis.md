![Inventory model](http://yuml.me/048a6c2b)

# Road map features (from [Beta Roadmap](https://docs.google.com/a/crowdint.com/document/d/1HBgbvvwyCKMf3cuxRIYjeTJli--qTaxDTXP4YegG-cE/edit#))
## Provisioning
- Store owner will configure the FoxComm ERP with a 3PL for Inventory integration and reporting
  - Store owner decides which 3PL they are going to use (hopefully with an API key or token)
  - Integration between FoxComm ERP and 3PL controls the communication and polling loops between the two systems
  - Thought: we need to have our own ERP abstraction
- The store owner creates a manifest and FoxComm transmits it to the 3PL
  - This may have migration implications when coming from other platforms (such as Spree and Magento)
- Store owner will set up shipping categories (default, hazardous) in Inventory
- Store owner will set up return rules (e.g. can be returned, return window, restocking fee) in Inventory
- FoxComm polls the 3PL to discover updates about the inventory and it’s levels
- FoxComm will notify the store owner when there is in an error syncing with the 3PL
- FoxComm will generate a polling report/dashboard about the connection with the 3PL

## Checking out
- Store customer can confirm checkout and place an order
  - A transaction lock is built on our inventory levels so the order can be assured.
  - Order is placed with 3PL to organise delivery.

# Previous Features (from SFO discussion)

- An admin CRUDs Stock Locations
  - An admin can add inventory to a stock location
  - An admin can remove an item from inventory
  - A 3PL can add inventory to a stock location
  - A 3PL can remove an item from inventory
  - Items can be transferred across stock locations
- Inventory management
  - An admin can view inventory in a stock location
  - An admin can view held inventory
  - Inventory can be held and even associated to a temporal order
  - A user can buy a product that has an in_stock value that is > 0
  - Real time inventory API
- An admin can create a shipping rule
  - Priorities (global ranking of stock locations)
  - Rules per item
  - Rules per user (internal + external)
  - Load balancing
  - Split shipments
- Notifications
  - An admin can create stock notifications
- Analytics
  - An admin can see sales velocity
- Returns
  - A user can return a product
- Shipping methods
  - An admin CRUDs a shipping method
- 3PL integration
  - A users order is transmitted to a 3PL

# Stories

## Returns
- A user tells the system that he wants a certain product to be returned
- Something schedules a pick up
- The product is received 
- The product is inspected
- The product may be accepted or rejected
- If the product is accepted a refund may be issued
- If the product is accepted it has to be entered in the system
- It can be entered as a new stock item
- or it can be entered as a new variant/product (think refurbished)
- the product is available again