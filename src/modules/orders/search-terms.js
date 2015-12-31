const searchTerms = [
  {
    title: 'Order',
    type: 'object',
    options: [
      {
        title: 'ID',
        type: 'string',
        term: 'id'
      }, {
        title: 'State',
        type: 'enum',
        term: 'status',
        suggestions: [
          { display: 'Cart', value: 'cart' },
          { display: 'Remorse Hold', value: 'remorseHold' },
          { display: 'Manual Hold', value: 'manualHold' },
          { display: 'Fraud Hold', value: 'fraudHold' },
          { display: 'Fulfillment Started', value: 'fulfillmentStarted' },
          { display: 'Shipped', value: 'shipped' },
          { display: 'Partially Shipped', value: 'partiallyShipped' },
          { display: 'Canceled', value: 'canceled' }
        ]
      }, {
        title: 'Date Placed',
        type: 'date',
        term: 'placedAt'
      }
    ]
  }, {
    title: 'Shipment',
    type: 'object',
    options: [
      {
        title: 'State',
        type: 'enum',
        term: 'shipments.status',
        suggestions: [
          { display: 'Shipped', value: 'shipped' },
          { display: 'Partially Shipped', value: 'partiallyShipped' },
          { display: 'Delivered', value: 'delivered' }
        ]
      }, {
        title: 'Method',
        type: 'string',
        term: 'shipments.admin_display_name',
      }, {
        title: 'Cost',
        type: 'number',
        term: 'shipments.shipping_price'
      }, {
        title: 'Total Number',
        type: 'number'
      }
    ]
  }, {
    title: 'Customer',
    type: 'object',
    options: [
      {
        title: 'Name',
        type: 'string',
        term: 'customer.name'
      },
      {
        title: 'Email',
        type: 'string',
        term: 'customer.email'
      },
      {
        title: 'Total Sales',
        type: 'number',
        term: 'customer.revenue'
      },
      {
        title: 'Blacklist Status',
        type: 'bool',
        term: 'customer.is_blacklisted'
      },
      {
        title: 'Date Joined',
        type: 'date',
        term: 'customer.joined_at'
      }
    ]
  }, {
    title: 'Shipping',
    type: 'object',
    options: [
      {
        title: 'City',
        type: 'string',
        term: 'shippingAddresses.city'
      },
      {
        title: 'State',
        type: 'string',
        term: 'shippingAddresses.region'
      },
      {
        title: 'Zip',
        type: 'string',
        term: 'shippingAddresses.zip'
      }
    ]
  }, {
    title: 'Billing',
    type: 'object',
    options: [
      {
        title: 'City',
        type: 'string',
        term: 'billingAddresses.city'
      },
      {
        title: 'State',
        type: 'string',
        term: 'billingAddresses.region'
      },
      {
        title: 'Zip',
        type: 'string',
        term: 'billingAddresses.zip'
      }
    ]
  }, {
    title: 'Items',
    type: 'object',
    options: [
      {
        title: 'Total Number',
        type: 'number'
      },
      {
        title: 'Product Name',
        type: 'string',
        term: 'lineItems.name'
      },
      {
        title: 'Product SKU',
        type: 'string',
        term: 'lineItems.sku'
      }
    ]
  }, {
    title: 'Assignee',
    type: 'enum',
    term: 'assignees',
    suggestions: [
      { display: 'Has Assignee', value: 'true' },
      { display: 'Has No Assignee', value: 'false' }
    ]
  }
];

export default searchTerms;
