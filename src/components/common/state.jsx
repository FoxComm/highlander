import React, { PropTypes } from 'react';
import { get } from 'sprout-data';

import { stateTitles as orderStateTitles } from '../../paragons/order';

export const states = {
  order: orderStateTitles,
  payment: {
    cart: 'Cart'
  },
  shipment: {
    cart: 'Cart',
    ordered: 'Ordered',
    manualHold: 'Manual Hold',
    remorseHold: 'Remorse Hold',
    fraudHold: 'Fraud Hold',
    fulfillmentStarted: 'Fulfillment Started',
    partiallyShipped: 'Partially Shipped',
    shipped: 'Shipped'
  },
  rma: {
    pending: 'Pending'
  },
  storeCreditTransaction: {
    capture: 'Captured',
    onHold: 'On Hold',
    active: 'Active'
  },
  giftCard: {
    csrAppeasement: 'Appeasement',
    customerPurchase: 'Customer Purchase',
    fromStoreCredit: 'From Store Credit',
    rmaProcess: 'RMA Process',
    cart: 'Cart',
    active: 'Active',
    fullyRedeemed: 'Fully Redeemed',
    canceled: 'Canceled'
  }
};

const State = (props) => {
  return <span className="fc-status">{get(statuses, [props.model, props.value], '[Invalid]')}</span>;
};

State.propTypes = {
  value: PropTypes.string.isRequired,
  model: PropTypes.string.isRequired,
};

export default State;
