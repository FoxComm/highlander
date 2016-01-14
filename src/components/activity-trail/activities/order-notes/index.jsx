
import React from 'react';
import types from '../base/types';
import OrderTarget from '../base/order-target';

const representatives = {
  [types.ORDER_NOTE_CREATED]: {
    title: data => {
      return (
        <span>
          <strong>added a note</strong> on <OrderTarget order={data.order}/>.
        </span>
      );
    },
    details: data => {
      return {
        previous: null,
        newOne: data.text,
      };
    },
  },
  [types.ORDER_NOTE_UPDATED]: {
    title: data => {
      return (
        <span>
          <strong>changed a note</strong> on <OrderTarget order={data.order}/>.
        </span>
      );
    },
    details: data => {
      return {
        previous: data.oldText,
        newOne: data.newText,
      };
    },
  },
  [types.ORDER_NOTE_DELETED]: {
    title: data => {
      return (
        <span>
          <strong>removed a note</strong> from <OrderTarget order={data.order}/>.
        </span>
      );
    },
    details: data => {
      return {
        previous: data.text,
        newOne: null,
      };
    },
  },
};

export default representatives;
