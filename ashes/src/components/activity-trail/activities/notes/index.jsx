import React from 'react';
import _ from 'lodash';
import types from '../base/types';
import CordLink from '../base/cord-link';
import CustomerLink from '../base/customer-link';
import GiftCardLink from '../base/gift-card-link';
import ProductLink from '../base/product-link';
import SkuLink from '../base/sku-link';
import PromotionLink from '../base/promotion-link';
import CouponLink from '../base/coupon-link';
import Title from '../base/title';

function linkForNoteTarget(data) {
  const name = _.get(data, 'entity.attributes.title.v');
  const attrs = data.entity.attributes;

  switch(data.note.referenceType) {
    case 'customer':
      return <CustomerLink customer={data.entity} />;
    case 'order':
      return <CordLink cord={data.entity} />;
    case 'giftCard':
      return <GiftCardLink code={data.entity.code} />;
    case 'product':
      return <ProductLink id={data.note.referenceId} context='default' name={name} />;
    case 'sku':
      return <SkuLink code={attrs.code.v} name={attrs.title.v} />;
    case 'promotion':
      return <PromotionLink id={data.note.referenceId} name={data.entity.attributes.name.v} />;
    case 'coupon':
      return <CouponLink id={data.note.referenceId} name={data.entity.attributes.name.v} />;
  }
  console.warn(`${data.note.referenceType} isn't supported yet`);
  console.dir(data);
  return `${data.note.referenceType}\<${data.note.referenceId}\>`;
}

const representatives = {
  [types.NOTE_CREATED]: {
    title: (data, activity) => {
      const target = linkForNoteTarget(data);
      const targetType = _.upperFirst(data.note.referenceType);

      return (
        <Title activity={activity}>
          <strong>added a note</strong> to {targetType}&nbsp;{target}
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
      const target = linkForNoteTarget(data);
      const targetType = _.upperFirst(data.note.referenceType);

      return (
        <Title activity={activity}>
          <strong>changed a note</strong> on {targetType}&nbsp;{target}
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
      const target = linkForNoteTarget(data);
      const targetType = _.upperFirst(data.note.referenceType);

      return (
        <Title activity={activity}>
          <strong>removed a note</strong> from {targetType}&nbsp;{target}
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
