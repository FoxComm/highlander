/* @flow */

import React, { Component, Element } from 'react';

import ContentBox from 'components/content-box/content-box';
import SkuLineItems from 'components/sku-line-items/sku-line-items';

import type { SkuItem } from 'paragons/order';

type Props = {
  order: {
    lineItems: {
      skus: Array<SkuItem>,
    },
  },
};

const OrderLineItems = (props: Props): Element => {
  const { skus } = props.order.lineItems;

  return (
    <ContentBox className="fc-line-items" title="Items" indentContent={false}>
      <SkuLineItems items={skus} withAttributes />
    </ContentBox>
  );
};

export default OrderLineItems;
