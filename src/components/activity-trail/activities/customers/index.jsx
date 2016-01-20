
import React from 'react';
import types from '../base/types';

import makeCustomerDesc from './template';

import * as updatedDesc from './updated';

const representatives = {
  [types.CUSTOMER_UPDATED]: updatedDesc,
  [types.CUSTOMER_CREATED]: makeCustomerDesc('created new'),
  [types.CUSTOMER_REGISTERED]: makeCustomerDesc('registered new'),
  [types.CUSTOMER_ACTIVATED]: makeCustomerDesc('activated'),
  [types.CUSTOMER_BLACKLISTED]: makeCustomerDesc(customerLink => {
    return (<span><strong>added</strong> customer {customerLink} <strong>to the blacklist</strong></span>);
  }),
  [types.CUSTOMER_REMOVED_FROM_BLACKLIST]: makeCustomerDesc(customerLink => {
    return (<span><strong>removed</strong> customer {customerLink} <strong>from the blacklist</strong></span>);
  }),
  [types.CUSTOMER_ENABLED]: makeCustomerDesc('enabled'),
  [types.CUSTOMER_DISABLED]: makeCustomerDesc('disabled'),
};

export default representatives;
