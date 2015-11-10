function formatCurrency(num) {
  if (num === null || num === void 0) return null;
  num = num.toString();
  let
    dollars = num.slice(0, -2),
    cents   = num.slice(-2);
  dollars = dollars.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `$${dollars}.${cents}`;
}

const orderStatuses = {
  cart: 'Cart',
  remorseHold: 'Remorse Hold',
  manualHold: 'Manual Hold',
  fraudHold: 'Fraud Hold',
  fulfillmentStarted: 'Fulfillment Started',
  canceled: 'Canceled',
  partiallyShipped: 'Partially Shipped',
  shipped: 'Shipped'
};

const rmaStatuses = {
  pending: 'Pending'
};

export {
  formatCurrency,
  orderStatuses,
  rmaStatuses
};
