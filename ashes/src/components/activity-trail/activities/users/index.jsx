
import React from 'react';
import types from '../base/types';

import makeUserDesc from './template';

import * as updatedDesc from './updated';

const representatives = {
  [types.USER_UPDATED]: updatedDesc,
  [types.USER_CREATED]: makeUserDesc('created new'),
  [types.USER_REGISTERED]: makeUserDesc('registered new'),
  [types.USER_ACTIVATED]: makeUserDesc('activated'),
  [types.CUSTOMER_UPDATED]: updatedDesc,
  [types.CUSTOMER_CREATED]: makeUserDesc('created new'),
  [types.CUSTOMER_REGISTERED]: makeUserDesc('registered new'),
  [types.CUSTOMER_ACTIVATED]: makeUserDesc('activated'),
  [types.USER_BLACKLISTED]: makeUserDesc(customerLink => {
    return (<span><strong>added</strong> customer {customerLink} <strong>to the blacklist</strong></span>);
  }),
  [types.USER_REMOVED_FROM_BLACKLIST]: makeUserDesc(customerLink => {
    return (<span><strong>removed</strong> customer {customerLink} <strong>from the blacklist</strong></span>);
  }),
  [types.USER_ENABLED]: makeUserDesc('enabled'),
  [types.USER_DISABLED]: makeUserDesc('disabled'),
};

export default representatives;
