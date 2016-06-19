# middlewarehouse

_middlewarehouse_ is our middleware Warehouse Management System (WMS) that is
designed to interact with our Order Management System (OMS) in Phoenix and
the external 3rd-party logistics (3PL), warehouses, WMSs, and shipping
services.

## Requirements

**Speed**

At it's core, _middlewarehouse_ is focused on reading and updating inventory
quanties exceedingly fast. The service must handle high levels of concurrent
traffic so that inventory dispositions can be quickly updated.

All other actions that may be helpful, but unnecessary to the process of
recording inventory changes (such as maintaining inventory summaries and audit
logs) will be asynchronous operations that use Green River.

**Inventory Source of Truth**

Representations of all the different stock locations (physical places where
inventory is stored) live inside _middlewarehouse_. By having visibility into
the type and quantity of inventory in each stock location, the system will
maintain an accurate representation of current inventory levels at all times.

**Shipping Management**

The creation of shipments, including the logic for split shipments, lives
inside this service.

## API Operations

- Modify counts
- Try to reserve some set of SKUs
- Try to unreserve some set of SKUs