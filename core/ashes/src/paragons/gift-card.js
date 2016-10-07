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
  giftCardTransfer: 'giftCardTransfer',
  customerPurchase: 'customerPurchase',
  custom: 'custom',
};

export const typeTitles = {
  [types.csrAppeasement]: 'CSR Appeasement',
  [types.giftCardTransfer]: 'Gift Card Transfer',
  [types.customerPurchase]: 'Customer Purchase',
  [types.custom]: 'Custom',
};

export const stateActionTitles = {
  [states.active]: 'Activate',
  [states.canceled]: 'Cancel Gift Card',
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
  (giftCard, transitions) => {
    if (giftCard.availableBalance !== giftCard.currentBalance){
      return transitions.filter(state => state != states.canceled);
    }

    return transitions;
  },
];

export const getStateTransitions = giftCard => {
  let transitions = stateTransitions[giftCard.state];

  stateTransitionsFilters.forEach(filter => {
    transitions = filter(giftCard, transitions);
  });

  return transitions;
};
