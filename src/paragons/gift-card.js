import _ from 'lodash';

export const states = {
  active: 'active',
  canceled: 'canceled',
  cart: 'cart',
  fullyRedeemed: 'fullyRedeemed',
  onHold: 'onHold',
};

export const stateTitles = {
  [states.active]: 'Active',
  [states.canceled]: 'Canceled',
  [states.cart]: 'Cart',
  [states.fullyRedeemed]: 'Fully Redeemed',
  [states.onHold]: 'On Hold',
};

export const stateActionTitles= {
  [states.active]: 'Activate',
  [states.canceled]: 'Cancel Gift Card',
  [states.cart]: 'Add to Cart',
  [states.fullyRedeemed]: 'Redeem',
  [states.onHold]: 'Hold',
};

export const stateTransitions = {
  active: [states.onHold, states.canceled],
  onHold: [states.active, states.canceled],
  canceled: [],
  fullyRedeemed: [],
  cart: [],
};

