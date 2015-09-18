'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import SkuStore from './sku-store';
import SkuResult from './sku-result';
import Typeahead from '../typeahead/typeahead';
import LineItemCounter from './line-item-counter';
import DeleteLineItem from './line-item-delete';

const defaultColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'quantity', text: 'Quantity'},
  {field: 'total', text: 'Total', type: 'currency'}
];

const editColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'lineItem', text: 'Quantity', component: 'LineItemCounter'},
  {field: 'total', text: 'Total', type: 'currency'},
  {field: 'delete', text: 'Delete', component: 'DeleteLineItem'}
];

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

  itemSelected(sku) {
    if (this.props.onChange) {
      this.props.onChange([{'sku': sku.sku, 'quantity': 1}]);
    }
  }

  render() {
    let order = this.props.order;
    let editing = this.state.isEditing;
    let typeahead = null;
    let body = null;
    let columns = null;
    let actions = null;
    let editButton = null;
    let addItem = null;

    if (editing) {
      columns = editColumns;
      typeahead = <Typeahead component={SkuResult} store={SkuStore} selectEvent="addLineItem" />
      body = (
        <TableBody columns={columns} rows={order.lineItems} model='lineItem'>
          <LineItemCounter onChange={this.props.onChange} />
          <DeleteLineItem onDelete={this.props.onChange} />
        </TableBody>
      );
      addItem = (
        <div>
          <strong>Add Item</strong>
          <Typeahead callback={this.itemSelected.bind(this)} component={SkuResult} store={SkuStore} />
        </div>
      );
      actions = (
        <footer>
          <button className="fc-btn fc-btn-primary" onClick={this.toggleEdit.bind(this)}>Done</button>
        </footer>
      );
    } else {
      columns = defaultColumns;
      body = <TableBody columns={columns} rows={order.lineItems} model='line-item'/>;
      editButton = (
        <button className="fc-btn" onClick={this.toggleEdit.bind(this)}>
          <i className="fa fa-pencil"></i>
        </button>
      );
    }

    return (
      <section className="fc-content-box order-line-items">
        <header>
          <div className='fc-grid'>
            <div className="fc-col-2-3">Items</div>
            <div className="fc-col-1-3">
              {editButton}
            </div>
          </div>
        </header>
        <table className="fc-table">
          <TableHead columns={columns}/>
          {body}
        </table>
        {addItem}
        {actions}
      </section>
    );
  }
}

OrderLineItems.propTypes = {
  order: React.PropTypes.object,
  onChange: React.PropTypes.func
};
