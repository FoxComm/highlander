import React from 'react';
import types from '../base/types';

import Title from '../base/title';

const representatives = {
  [types.FULL_PRODUCT_CREATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>created product</strong>
        </Title>
      );
    }
  },
  [types.FULL_PRODUCT_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>updated product</strong>
        </Title>
      );
    }
  },
};

export default representatives;
