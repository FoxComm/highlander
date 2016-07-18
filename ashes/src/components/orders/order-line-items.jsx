/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';

import ContentBox from 'components/content-box/content-box';
import OrderLineItem from './order-line-item';
import TableView from 'components/table/tableview';

const columns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'quantity', text: 'Qty'},
  {field: 'totalPrice', text: 'Total', type: 'currency'}
];

// TODO: Generate a real typed object here.
type Props = {
  order: Object,
};

export default class OrderLineItems extends Component {
  props: Props;

  collectLineItems(skus: Array<Object>): Array<Object> {
    let uniqueSkus = {};
    const items = _.transform(skus, (result, lineItem) => {
      const sku = lineItem.sku;
      if (_.isNumber(uniqueSkus[sku])) {
        const qty = result[uniqueSkus[sku]].quantity += 1;
        result[uniqueSkus[sku]].totalPrice = lineItem.price * qty;
      } else {
        uniqueSkus[sku] = result.length;
        result.push({ ...lineItem, quantity: 1 });
      }
    });
    return items;
  }

  render() {
    const items = _.get(this.props, 'order.lineItems.skus', []);

    let viewContent = null;
    if (items.length > 0) {
      const collectedItems = this.collectLineItems(items);
      viewContent = (
        <TableView
          columns={columns}
          emptyMessage="No items yet."
          data={{rows: collectedItems}} />
      );
    } else {
      viewContent = (
        <div className='fc-content-box__empty-text'>
          No items yet.
        </div>
      );
    }

    return (
      <ContentBox
        className='fc-line-items'
        title="Items"
        indentContent={false}
        viewContent={viewContent} />
    );
  }
}
