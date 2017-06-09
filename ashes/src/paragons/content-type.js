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

export const types = {
  csrAppeasement: 'csrAppeasement',
  contentTypeTransfer: 'contentTypeTransfer',
  customerPurchase: 'customerPurchase',
  custom: 'custom',
};

export const typeTitles = {
  [types.csrAppeasement]: 'CSR Appeasement',
  [types.contentTypeTransfer]: 'Content Type Transfer',
  [types.customerPurchase]: 'Customer Purchase',
  [types.custom]: 'Custom',
};

export const stateActionTitles = {
  [states.active]: 'Activate',
  [states.canceled]: 'Cancel Content Type',
  [states.cart]: 'Add to Cart',
  [states.fullyRedeemed]: 'Redeem',
  [states.onHold]: 'Hold',
};

const stateTransitions = {
  [states.active]: [states.onHold, states.canceled],
  [states.onHold]: [states.active, states.canceled],
  [states.canceled]: [],
  [states.fullyRedeemed]: [],
  [states.cart]: [],
};

const stateTransitionsFilters = [
  (contentType, transitions) => {
    if (contentType.availableBalance !== contentType.currentBalance){
      return transitions.filter(state => state != states.canceled);
    }

    return transitions;
  },
];

export const getStateTransitions = contentType => {
  let transitions = stateTransitions[contentType.state];

  stateTransitionsFilters.forEach(filter => {
    transitions = filter(contentType, transitions);
  });

  return transitions;
};
