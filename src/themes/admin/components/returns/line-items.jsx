'use strict';

import React from 'react';
import TableView from '../tables/tableview';

export default class ReturnLineItems extends React.Component {
  render() {
    let retrn = this.props.return;

    return (
      <section className="fc-content-box return-line-items">
        <header className="header">
          <span>Items</span>
        </header>
        <TableView
          columns={this.props.tableColumns}
          rows={retrn.lineItems}
          model='return'
          />
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
    {field: 'sku', text: 'SKU'},
    {field: 'qty', text: 'Quantity'},
    {field: 'inventoryDisposition', text: 'Inventory Disposition'},
    {field: 'refund', text: 'Refund', type: 'currency'},
    {field: 'reason', text: 'Reason'}
  ]
};
