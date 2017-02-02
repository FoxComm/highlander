import React, { PropTypes } from 'react';
import { get } from 'sprout-data';

import { stateTitles as orderStateTitles } from '../../paragons/order';

export const states = {
  order: orderStateTitles,
  payment: {
    cart: 'Cart',
    auth: 'Auth',
    failedAuth: 'Failed Auth',
    expiredAuth: 'Expired Auth',
    canceledAuth: 'Canceled Auth',
    partialCapture: 'Partial Capture',
    failedCapture: 'Failed Capture',
    fullCapture: 'Full Capture',
  },
  shipment: {
    ...orderStateTitles,
    ordered: 'Ordered',
  },
  rma: {
    pending: 'Pending'
  },
  storeCreditTransaction: {
    capture: 'Captured',
    onHold: 'On Hold',
    active: 'Active',
    canceled: 'Canceled',
  },
  giftCard: {
    cart: 'Cart',
    active: 'Active',
    onHold: 'On Hold',
    fullyRedeemed: 'Fully Redeemed',
    canceled: 'Canceled',
  },
  storeCredit: {
    cart: 'Cart',
    active: 'Active',
    onHold: 'On Hold',
    fullyRedeemed: 'Fully Redeemed',
    canceled: 'Canceled',
  },
  sku: {
    backorder: 'Backorder',
    sellable: 'Sellable',
    preorder: 'Preorder',
    nonsellable: 'Non-sellable',
  },
  skuState: {
    onHand: 'OnHand',
    onHold: 'OnHold',
    reserved: 'Reserved',
  }
};

const State = (props) => {
  return <span id={props.stateId} className="fc-model-state">{get(states, [props.model, props.value], '[Invalid]')}</span>;
};

State.propTypes = {
  value: PropTypes.string.isRequired,
  model: PropTypes.string.isRequired,
  stateId: PropTypes.string,
};

export function formattedStatus(status) {
  switch (status) {
    case 'onHold':
      return 'On Hold';
    case 'active':
      return 'Active';
    default:
      return status;
  }
}

export default State;
