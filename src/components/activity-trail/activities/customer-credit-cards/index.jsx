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
        newOne: <PaymentMethod paymentMethod={data.creditCard} />,
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
        previous: <PaymentMethod paymentMethod={data.creditCard} />
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
        newOne: <PaymentMethod paymentMethod={data.newInfo} />,
        previous: <PaymentMethod paymentMethod={data.oldInfo} />
      };
    }
  },
};

export default representatives;
