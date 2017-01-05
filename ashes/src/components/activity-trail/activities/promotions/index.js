import React from 'react';
import types from '../base/types';

import Title from '../base/title';

const representatives = {
  [types.PROMOTION_CREATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>created promotion</strong>
        </Title>
      );
    }
  },
  [types.PROMOTION_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>updated promotion</strong>
        </Title>
      );
    }
  },
};

export default representatives;
