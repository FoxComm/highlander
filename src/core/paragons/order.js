
export const states = {
  cart: 'cart',
  remorseHold: 'remorseHold',
  manualHold: 'manualHold',
  fraudHold: 'fraudHold',
  fulfillmentStarted: 'fulfillmentStarted',
  canceled: 'canceled',
  partiallyShipped: 'partiallyShipped',
  shipped: 'shipped',
};

export const stateTitles = {
  [states.cart]: 'Cart',
  [states.remorseHold]: 'Remorse Hold',
  [states.manualHold]: 'Manual Hold',
  [states.fraudHold]: 'Fraud Hold',
  [states.fulfillmentStarted]: 'Fulfillment Started',
  [states.canceled]: 'Canceled',
  [states.partiallyShipped]: 'Partially Shipped',
  [states.shipped]: 'Shipped',
};
