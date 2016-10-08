# Top Drawer WMS & Shipping MVP

## Scope

### Goals

- Optimize for a simple, fast implementation
- Implement against the FoxCommerce extension infrastructure
- Provides an interface to tell FoxCommerce when an order has shipped
- Allows the Top Drawer operations team to print shipping labels
- Provides an interface where the Top Drawer operations team can update inventory

### Non-Goals

- There will be no external warehouse at first launch, everything will be
  managed manually
- Returns won't be implemented at the time of launch
- Refunds should be managed directly in Stripe at launch

### Features

- Mark orders as shipped
- Update SKU inventory position
- Send shipment confirmation emails
- Generate tracking numbers
- Print shipping labels
