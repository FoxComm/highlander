
import React from 'react';
import types from '../base/types';
import OrderTarget from '../base/order-target';
import Title from '../base/title';

const representatives = {
  [types.ORDER_NOTE_CREATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>added a note</strong> to <OrderTarget order={data.order}/>
        </Title>
      );
    },
    details: data => {
      return {
        previous: null,
        newOne: data.note.body,
      };
    },
  },
  [types.ORDER_NOTE_UPDATED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>changed a note</strong> on <OrderTarget order={data.order}/>
        </Title>
      );
    },
    details: data => {
      return {
        previous: data.oldNote.body,
        newOne: data.note.body,
      };
    },
  },
  [types.ORDER_NOTE_DELETED]: {
    title: (data, activity) => {
      return (
        <Title activity={activity}>
          <strong>removed a note</strong> from <OrderTarget order={data.order}/>
        </Title>
      );
    },
    details: data => {
      return {
        previous: data.note.body,
        newOne: null,
      };
    },
  },
};

export default representatives;
