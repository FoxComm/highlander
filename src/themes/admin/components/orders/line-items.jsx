'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import SkuStore from './sku-store';
import SkuResult from './sku-result';

export default class OrderLineItems extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isEditing: false
    };
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  render() {
    let order = this.props.order;
    let editing = this.state.isEditing;
    let typeahead = null;

    if (editing) {
      typeahead = <Typeahead component={SkuResult} store={SkuStore} selectEvent="addLineItem" />
    }
    return (
      <section id="order-line-items">
        <header>
          <div className='fc-grid'>
            <div className="fc-col-2-3">Items</div>
            <div className="fc-col-1-3"><button className="fc-button" onClick={this.toggleEdit.bind(this)}><i className="fa fa-pencil"></i></button></div>
          </div>
        </header>
        <table className="fc-table">
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
