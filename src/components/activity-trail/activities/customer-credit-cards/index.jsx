import React from 'react';
import types from '../base/types';

import CustomerLink from '../base/customer-link';
import PaymentMethod from '../../../payment/payment-method';


const representatives = {
  [types.CREDIT_CARD_ADDED]: {
    title: data => {
      return (
        <span>
          <strong>created a credit card</strong> for customer <CustomerLink customer={data.customer} />.
        </span>
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
    title: data => {
      return (
        <span>
          <strong>removed a credit card</strong> for customer <CustomerLink customer={data.customer} />.
        </span>
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
    title: data => {
      return (
        <span>
          <strong>changed a credit card</strong> for customer <CustomerLink customer={data.customer} />.
        </span>
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
