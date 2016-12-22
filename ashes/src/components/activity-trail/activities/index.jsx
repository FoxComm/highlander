
import React, { PropTypes } from 'react';

import Activity from './base/activity';

import ordersActivities from './orders';
import notesActivities from './notes';
import orderShippingAddressActivities from './order-shipping-address';
import orderShippingMethodsActivities from './order-shipping-methods';
import orderPaymentMethodsActivities from './order-payment-methods';
import orderLineItemsActivities from './order-line-items';
import usersActivities from './users';
import userAddressesActivities from './user-addresses';
import userCreditCardsActivities from './user-credit-cards';
import giftCardsActivities from './gift-cards';
import storeCreditsActivities from './store-credits';
import assignmentsActivities from './assignments';
import productsActivities from './products';
import skusActivities from './skus';
import couponActivities from './coupons';
import promotionActivities from './promotions';

export const representatives = {
  ...ordersActivities,
  ...notesActivities,
  ...orderShippingAddressActivities,
  ...orderShippingMethodsActivities,
  ...orderPaymentMethodsActivities,
  ...orderLineItemsActivities,
  ...usersActivities,
  ...userAddressesActivities,
  ...userCreditCardsActivities,
  ...giftCardsActivities,
  ...storeCreditsActivities,
  ...assignmentsActivities,
  ...productsActivities,
  ...skusActivities,
  ...couponActivities,
  ...promotionActivities,
};

export function getActivityRepresentative(activity) {
  const desc = representatives[activity.kind];

  if (!desc) return null;

  const args = [activity.data, activity];

  const params = {
    title: desc.title(...args),
    details: desc.details ? desc.details(...args) : null,
  };

  return props => <Activity {...params} {...props} />;
}

const ActivityRepresentativeWrapper = (props) => {
  const ActivityRepresentative = getActivityRepresentative(props.activity);

  if (!ActivityRepresentative) return null;

  return <ActivityRepresentative {...props} />;
};

ActivityRepresentativeWrapper.propTypes = {
  activity: PropTypes.object.isRequired,
};

export default ActivityRepresentativeWrapper;
