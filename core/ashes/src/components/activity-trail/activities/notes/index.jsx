import React from 'react';
import types from '../base/types';
import OrderTarget from '../base/order-target';
import Title from '../base/title';

const representatives = {
  [types.NOTE_CREATED]: {
    title: (data, activity) => {
      const order = { title: 'Order', referenceNumber: data.entity.referenceNumber };

      return (
        <Title activity={activity}>
          <strong>added a note</strong> to <OrderTarget order={order} />
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
  [types.NOTE_UPDATED]: {
    title: (data, activity) => {
      const order = { title: 'Order', referenceNumber: data.entity.referenceNumber };

      return (
        <Title activity={activity}>
          <strong>changed a note</strong> on <OrderTarget order={order} />
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
  [types.NOTE_DELETED]: {
    title: (data, activity) => {
      const order = { title: 'Order', referenceNumber: data.entity.referenceNumber };

      return (
        <Title activity={activity}>
          <strong>removed a note</strong> from <OrderTarget order={order} />
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
