/* @flow */

import React, { Component, Element } from 'react';

import ContentBox from 'components/content-box/content-box';
import SkuLineItems from 'components/sku-line-items/sku-line-items';

import type { SkuItem } from 'paragons/order';
import OrderParagon from 'paragons/order';

type Props = {
  order: OrderParagon;
};

const OrderLineItems = (props: Props) => {
  const { skus } = props.order.lineItems;

  return (
    <ContentBox className="fc-line-items" title="Items" indentContent={false}>
      <SkuLineItems items={skus} withAttributes />
    </ContentBox>
  );
};

export default OrderLineItems;
