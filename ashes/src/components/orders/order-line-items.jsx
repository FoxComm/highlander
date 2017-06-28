/* @flow */

import { findIndex } from 'lodash';
import React from 'react';

import ContentBox from 'components/content-box/content-box';
import SkuLineItems, { defaultColumns } from 'components/sku-line-items/sku-line-items';

import OrderParagon, { isAmazonOrder } from 'paragons/order';

type Props = {
  order: OrderParagon,
};

const OrderLineItems = (props: Props) => {
  const { skus } = props.order.lineItems;

  let columns = defaultColumns;

  if (isAmazonOrder(props.order)) {
    const skuFieldIndex = findIndex(defaultColumns, { field: 'sku' });

    columns = [
      ...defaultColumns.slice(0, skuFieldIndex),
      {
        field: 'sku',
        text: 'SKU',
      },
      ...defaultColumns.slice(skuFieldIndex + 1),
    ];
  }

  return (
    <ContentBox className="fc-line-items" title="Items" indentContent={false}>
      <SkuLineItems items={skus} columns={columns} withAttributes />
    </ContentBox>
  );
};

export default OrderLineItems;
