
import React, { PropTypes } from 'react';

import Activity from './base/activity';

import ordersActivities from './orders';
import notesActivities from './notes';
import orderShippingAddressActivities from './order-shipping-address';
import orderShippingMethodsActivities from './order-shipping-methods';
import orderPaymentMethodsActivities from './order-payment-methods';
import orderLineItemsActivities from './order-line-items';
import customersActivities from './customers';
import customerAddressesActivities from './customer-addresses';
import customerCreditCardsActivities from './customer-credit-cards';
import giftCardsActivities from './gift-cards';
import storeCreditsActivities from './store-credits';
import assignmentsActivities from './assignments';
import productsActivities from './products';
import skusActivities from './skus';

export const representatives = {
  ...ordersActivities,
  ...notesActivities,
  ...orderShippingAddressActivities,
  ...orderShippingMethodsActivities,
  ...orderPaymentMethodsActivities,
  ...orderLineItemsActivities,
  ...customersActivities,
  ...customerAddressesActivities,
  ...customerCreditCardsActivities,
  ...giftCardsActivities,
  ...storeCreditsActivities,
  ...assignmentsActivities,
  ...productsActivities,
  ...skusActivities,
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
