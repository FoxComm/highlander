/* @flow */

import React, { Element } from 'react';
import _ from 'lodash';

import TableView from 'components/table/tableview';

import type { SkuItem } from 'paragons/order';

const defaultColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'quantity', text: 'Qty'},
  {field: 'totalPrice', text: 'Total', type: 'currency'},
];

type Column = {
  field: string,
  text: string,
  type?: string,
};

type Props = {
  columns?: Array<Column>,
  items: Array<SkuItem>,
  renderRow?: Function,
};

const SkuLineItems = (props: Props): Element => {
  const { items, renderRow } = props;
  const columns = props.columns ? props.columns : defaultColumns;

  if (items.length > 0) {
    return (
      <TableView
        columns={columns}
        emptyMessage="No items yet."
        data={{rows: items}}
        renderRow={renderRow} />
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
