import React from 'react';
import types from '../base/types';

import Title from '../base/title';

const representatives = {
  [types.COUPON_CREATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>created coupon</strong>
        </Title>
      );
    }
  },
  [types.COUPON_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>updated coupon</strong>
        </Title>
      );
    }
  },
};

export default representatives;
