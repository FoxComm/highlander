/* @flow */

import _ from 'lodash';
import classNames from 'classnames';
import React, { Element } from 'react';

import TableView from 'components/table/tableview';
import Link from 'components/link/link';
import SkuLineItemAttributes from '../sku-line-items/sku-line-item-attributes';

import type { SkuItem } from 'paragons/order';

export const defaultColumns = [
  { field: 'imagePath', text: 'Image', type: 'image' },
  { field: 'name', text: 'Name' },
  {
    field: 'sku',
    text: 'SKU',
    render: (code: string) => <Link to="sku-details" params={{ skuCode: code }}>{code}</Link>
  },
  { field: 'price', text: 'Price', type: 'currency' },
  { field: 'quantity', text: 'Qty' },
  { field: 'totalPrice', text: 'Total', type: 'currency' },
];

const attributesColumns = {
  'giftCard': [
    {
      field: 'code',
      text: 'Gift Card Number',
      render: code => !_.isEmpty(code) ? <Link to="giftcard" params={{ giftCard: code }}>{code}</Link> : 'N/A'
    },
    { field: 'recipientName', text: 'Recipient Name' },
    { field: 'recipientEmail', text: 'Recipient Email' },
    { field: 'senderName', text: 'Sender Name' },
    { field: 'message', text: 'Message', type: 'raw' },
  ],
};

type Column = {
  field: string,
  text: string,
  type?: string,
};

type Props = {
  columns?: Array<Column>,
  items: Array<SkuItem>,
  renderRow?: Function,
  withAttributes?: boolean,
  className?: string,
};

const lineItemAttributes = (item: SkuItem, columns: Array<Column>): Array<?Element<*>> => {
  const attributes = _.get(item, 'attributes', {});

  if (!_.isEmpty(attributes)) {
    return Object.keys(attributes).map((name: string) => (
      _.get(attributesColumns, name) ?
        <SkuLineItemAttributes
          spanNumber={columns.length}
          columns={attributesColumns[name]}
          data={{rows: [attributes[name]]}}
        /> : null
    ));
  }

  return [];
};

const SkuLineItems = (props: Props) => {
  const { items, renderRow, withAttributes, className } = props;
  const columns = props.columns ? props.columns : defaultColumns;

  const processRows = (rows, columns) =>
    _.flatMap(rows, (row, index) => {
      const attributes = _.get(items[index], 'attributes');
      const className = classNames({ '_with-attributes': !_.isEmpty(attributes) });

      return [
        React.cloneElement(row, { className }),
        lineItemAttributes(items[index], columns),
      ];
    });

  if (items.length > 0) {
    return (
      <TableView
        tbodyId="cart-line-items"
        className={className}
        columns={columns}
        emptyMessage="No items yet."
        data={{rows: items}}
        renderRow={renderRow}
        processRows={withAttributes ? processRows : _.identity}
      />
    );
  } else {
    return (
      <div className='fc-content-box__empty-text'>
        No items yet.
      </div>
    );
  }
};

export default SkuLineItems;
