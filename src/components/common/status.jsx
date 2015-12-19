import React, { PropTypes } from 'react';
import { get } from 'sprout-data';

import { statusTitles as orderStatuses } from '../../paragons/order';

export const statuses = {
  order: orderStatuses,
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
  }
};

const Status = (props) => {
  return <span className="fc-status">{get(statuses, [props.model, props.value], '[Invalid]')}</span>;
};

Status.propTypes = {
  value: PropTypes.string.isRequired,
  model: PropTypes.string.isRequired
};

export default Status;
