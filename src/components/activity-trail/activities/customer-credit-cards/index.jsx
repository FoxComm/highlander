import React from 'react';
import types from '../base/types';

import CustomerLink from '../base/customer-link';
import PaymentMethod from '../../../payment/payment-method';
import Title from '../base/title';


const representatives = {
  [types.CREDIT_CARD_ADDED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>created a credit card</strong>
        </Title>
      );
    },
    details: data => {
      return {
        newOne: <PaymentMethod card={data.creditCard} />,
        previous: null
      };
    }
  },
  [types.CREDIT_CARD_REMOVED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>removed a credit card</strong>
        </Title>
      );
    },
    details: data => {
      return {
        newOne: null,
        previous: <PaymentMethod card={data.creditCard} />
      };
    }
  },
  [types.CREDIT_CARD_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>changed a credit card</strong>
        </Title>
      );
    },
    details: data => {
      return {
        newOne: <PaymentMethod card={data.newInfo} />,
        previous: <PaymentMethod card={data.oldInfo} />
      };
    }
  },
};

export default representatives;
