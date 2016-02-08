import React, { PropTypes } from 'react';
import { get } from 'sprout-data';

import { stateTitles as orderStateTitles } from '../../paragons/order';

export const states = {
  order: orderStateTitles,
  payment: {
    cart: 'Cart'
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
    fullyRedeemed: 'Fully Redeemed',
    canceled: 'Canceled',
  },
};

const State = (props) => {
  return <span className="fc-model-state">{get(states, [props.model, props.value], '[Invalid]')}</span>;
};

State.propTypes = {
  value: PropTypes.string.isRequired,
  model: PropTypes.string.isRequired,
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
