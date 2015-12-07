import React, { PropTypes } from 'react';
import { get } from 'sprout-data';

export const statuses = {
  order: {
    cart: 'Cart',
    remorseHold: 'Remorse Hold',
    manualHold: 'Manual Hold',
    fraudHold: 'Fraud Hold',
    fulfillmentStarted: 'Fulfillment Started',
    canceled: 'Canceled',
    partiallyShipped: 'Partially Shipped',
    shipped: 'Shipped'
  },
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
