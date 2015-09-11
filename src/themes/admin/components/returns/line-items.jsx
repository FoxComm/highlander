'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

export default class ReturnLineItems extends React.Component {
  render() {
    let retrn = this.props.return;

    return (
      <section className="fc-content-box return-line-items">
        <header className="header">
          <span>Items</span>
        </header>
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={retrn.lineItems} model='order'/>
        </table>
      </section>
    );
  }
}

ReturnLineItems.propTypes = {
  return: React.PropTypes.object,
  tableColumns: React.PropTypes.array
};

ReturnLineItems.defaultProps = {
  tableColumns: [
    {field: 'imagePath', text: 'Image', type: 'image'},
    {field: 'name', text: 'Name'},
    {field: 'skuId', text: 'SKU'},
    {field: 'price', text: 'Price', type: 'currency'},
    {field: 'qty', text: 'Quantity'},
    {field: 'total', text: 'Total', type: 'currency'},
    {field: 'status', text: 'Shipping Status'}
  ]
};
