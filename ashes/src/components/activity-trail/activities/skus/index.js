import React from 'react';
import types from '../base/types';

import Title from '../base/title';

const representatives = {
  [types.FULL_SKU_CREATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>created SKU</strong>
        </Title>
      );
    }
  },
  [types.FULL_SKU_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>updated SKU</strong>
        </Title>
      );
    }
  },
};

export default representatives;
