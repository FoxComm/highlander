/* @flow */

import React, { Component, Element } from 'react';

import ContentBox from 'components/content-box/content-box';
import SkuLineItems from 'components/sku-line-items/sku-line-items';
import TableRow from 'components/table/row';
import TableCell from 'components/table/cell';
import Currency from '../common/currency';
import Link from 'components/link/link';

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

  const renderRow = (row: SkuItem, index: number) => {
    return (
      <TableRow key={row.skuId}>
        <TableCell><img src={row.imagePath} /></TableCell>
        <TableCell>{row.name}</TableCell>
        <TableCell><Link to="sku-details" params={{skuId: row.skuId}}>{row.skuCode}</Link></TableCell>
        <TableCell><Currency value={row.price} /></TableCell>
        <TableCell>{row.quantity}</TableCell>
        <TableCell><Currency value={row.totalPrice} /></TableCell>
      </TableRow>
    );
  };

  return (
    <ContentBox className="fc-line-items" title="Items" indentContent={false}>
      <SkuLineItems items={skus} renderRow={renderRow} />
    </ContentBox>
  );
};

export default OrderLineItems;
