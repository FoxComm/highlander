'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import LineItemManager from './line-item-manager';
import DeleteLineItem from './delete-line-item';
import Typeahead from '../typeahead/typeahead';
import SkuResult from './sku-result';
import SkuStore from './sku-store';
import { dispatch, listenTo, stopListeningTo } from '../../lib/dispatcher';

const defaultColumns = [
  {field: 'image', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'skuId', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'quantity', text: 'Quantity'},
  {field: 'total', text: 'Total', type: 'currency'},
  {field: 'status', text: 'Shipping Status'}
];

const editColumns = [
  {field: 'id', text: 'Select', type: 'checkbox'},
  {field: 'image', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'skuId', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'lineItem', text: 'Quantity', component: 'LineItemManager'},
  {field: 'total', text: 'Total', type: 'currency'},
  {field: 'status', text: 'Shipping Status'},
  {field: 'delete', text: 'Delete', component: 'DeleteLineItem'}
];

export default class OrderLineItems extends React.Component {
  constructor(props) {
    super(props);
    this.onAddLineItem = this.onAddLineItem.bind(this);
  }

  onAddLineItem(sku) {
    dispatch('updateLineItem', [{'skuId': sku.skuId, 'quantity': 1}]);
  }

  componentDidMount() {
    listenTo('add-line-item', this);
  }

  componentWillUnmount() {
    stopListeningTo('add-line-item', this);
  }

  render() {
    let
      order     = this.props.order,
      isEditing = this.props.isEditing,
      columns   = this.props.tableColumns,
      body      = null,
      addItem   = null;

    if (isEditing) {
      columns = editColumns;
      body = (
        <TableBody columns={columns} rows={order.lineItems} model='order'>
          <LineItemManager />
          <DeleteLineItem />
        </TableBody>
      );
      addItem = (
        <div className="add-item">
          <strong>Add Item</strong>
          <Typeahead component={SkuResult} store={SkuStore} selectEvent="addLineItem" />
        </div>
      );
    } else {
      columns = defaultColumns;
      body = <TableBody columns={columns} rows={order.lineItems} model='order'/>;
    }

    return (
      <section id="order-line-items">
        <header>Items</header>
        <table className="inline">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={order.lineItems} model='order'/>
        </table>
        {addItem}
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
