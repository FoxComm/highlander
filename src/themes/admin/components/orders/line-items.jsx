'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

export default class OrderLineItems extends React.Component {
  render() {
    let order = this.props.order;

    return (
      <section id="order-line-items">
        <header>Items</header>
        <table className="inline">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={order.lineItems} model='order'/>
        </table>
      </section>
    );
  }
}

OrderLineItems.propTypes = {
  order: React.PropTypes.object,
  tableColumns: React.PropTypes.array
};

OrderLineItems.defaultProps = {
  tableColumns: [
    {field: 'image', text: 'Image', type: 'image'},
    {field: 'name', text: 'Name'},
    {field: 'skuId', text: 'SKU'},
    {field: 'price', text: 'Price', type: 'currency'},
    {field: 'quantity', text: 'Quantity'},
    {field: 'total', text: 'Total', type: 'currency'},
    {field: 'status', text: 'Shipping Status'}
  ]
};
