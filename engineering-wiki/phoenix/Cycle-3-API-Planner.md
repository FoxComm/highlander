#### March 18, 2015  

## Agenda  
- Review work over the past week  
- Backlog recap and goals  
- Plan for next cycle  
- Availability  

## Review work over the past week  

**Emmanuel**  

- Trying to code the model around the inventory. But he wanted us to understand more of what he has in mind.  
- Had a meeting about the overall goals of inventory and Newgistics  
- Leveraged the experience from BeautyKind  
- Did some modeling and coding about the Newgistics work  

_Comments_  
- Nick: seems like this Newgistics work makes things a whole lot clearer  
- Emmanuel: yes, we understand more about what we can achieve in Newgistics  
- Nick: now we have a rough idea about lifecycle of inventory.  
  - To get the entire process from start-to-finish  
- Emmanuel: the next step could be to prototype this inside the FoxComm code  

**Herman**  

- Most of the time this week was spent on BeautyKind  
- Worked on some things in order creation
  - Finished the missing tests  
- Yesterday, did a really good exercise by creating a state machine diagram for the order steps  
  - This is in a GitHub issue
- Implemented the multierror library that was suggested by Jacques  
- Would like to move the diagrams to draw.io so that he can correctly  

_Comments_  
- We should assume that Herman isn't around this week  

**Jacques**

- Service architecture code  
- Helped on hiring with Adil/Jeff  
- Thinking about his time  
- Review code and PRs

**Nick**  

- Started on Sunday - need to have big enough tasks to be able to work without Jacques or Jeff correctly  
- Refactored the Makefile  
- A lot of high level  
  - Logging has landed  
- Been roadmapping with Jeff during the morning, then working on ideas for code and working on the service architecture

**Jeff**  

- Worked on the reference UI  
- Hiring stuff  
- A lot of roadmapping with Nick  

### FoxComm Goals  

- Solve increasing complexity with SMBs  
- Help customers use business model innovation to scale  

## Mini-roadmap  

- _The full roadmap isn't complete, but lives here:_ [https://github.com/FoxComm/foxcomm2.0/wiki/Product-Roadmap](https://github.com/FoxComm/foxcomm2.0/wiki/Product-Roadmap)  
- _Roadmap document composed by Nick/Jeff:_ [Beta Roadmap](https://docs.google.com/a/foxcommerce.com/document/d/1HBgbvvwyCKMf3cuxRIYjeTJli--qTaxDTXP4YegG-cE/edit#heading=h.9zrmeq2sfjfz)  

## Goals for this sprint  

- Service communication needs to be answered at a basic level  
- API goals
  - Inventory should have a basic model for inventory and their quantities
  - Order creation should be merged and running
  - We need PIM work to resume and standardize the output from the endpoints
- UI goals
  - Remove the last garbage (e.g., auth code, admin code, ugly Grunt) from the UI
  - It needs to be easy to run in the repo
  - Cart creation should be wired up to FoxComm 2.0

## Plan for next cycle  

### Suggested work by theme  

#### Service Architecture  

- Drive communication between bounded contexts to a place where it's merged into the main branch  
- Get more of the main service architecture refactor to land  
- **Operating theme**  
  - We should be racing to get a version of the API up that can be tested against an API  
  - Don't let perfect be the enemy of good  

#### Inventory  

- Construct the model for how inventory and stock levels are persisted
- Inventory endpoints for the product details page and checkout process should be built:  
  - Check inventory levels on the PDP
  - Check inventory levels when a product is added to a cart
  - Check inventory levels when a store customer starts the checkout process  
  - Display the available shipping methods based on the items in the shopping cart  
- Post order creation flow
  - Put items on hold when an order is created  
  - Create shipments  
  - Communicate with the 3PL
  - Message the store customer when the items ship

#### Transactions  

- Complete the basic order creation flow  
- Add the integration with Inventory system to do stock level checks throughout the checkout process  
- Begin integration with one credit card provider 
  - Build an abstraction for how payments are represented in the system  
  - Do one sample integration

#### Product Information Management (PIM)  

- Complete the endpoints related to basic product creation  
  - Create a physical product  
  - Include the support for 

## Availability  

- Emmanuel  
- Herman  
- Jacques  
- Nick  
- Jeff  

