'use strict';

import React from 'react';

import ConfirmationDialog from '../modal/confirmation-dialog';
import EditableTableView from '../tables/editable-table-view';
import OrderLineItem from './order-line-item';
import TableHead from '../tables/head';

const viewModeColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'quantity', text: 'Qty'},
  {field: 'totalPrice', text: 'Total', type: 'currency'}
];

const editModeColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'lineItem', text: 'Qty', component: 'LineItemCounter'},
  {field: 'totalPrice', text: 'Total', type: 'currency'},
  {field: 'delete', text: 'Delete', component: 'DeleteLineItem'}
];

let OrderLineItems = (props) => {
  let order = props.order.currentOrder;
  let lineItemsStatus = props.order.lineItems;

  if (lineItemsStatus.isEditing) {
    return renderEditMode(props);
  } else {
    return (
      <EditableTableView
        title='Items'
        editAction={() => props.orderLineItemsStartEdit()}
        columns={viewModeColumns}
        rows={lineItemsStatus.items} />
    );
  }
};

let renderEditMode = (state) => {
  let order = state.order.currentOrder;
  let lineItemsStatus = state.order.lineItems;

  let orderLineItems = lineItemsStatus.items.map((lineItem, idx) => {
    return <OrderLineItem key={`lineItem-${idx}`} item={lineItem} {...state} />;
  });

  // TODO: Re-add the Typeahead after Andrey's refactor is complete.
  return (
    <div>
      <section className='fc-line-items fc-content-box'>
        <table className='fc-table'>
          <TableHead columns={editModeColumns} />
          <tbody>
            {orderLineItems}
          </tbody>
        </table>
        <footer>
          <div>
            <strong>Add Item</strong>
          </div>
          <button className='fc-btn fc-btn-primary' onClick={() => state.orderLineItemsCancelEdit()}>Done</button>
        </footer>
      </section>
      <ConfirmationDialog
        isVisible={lineItemsStatus.isDeleting}
        header='Confirm'
        body='Are you sure you want to delete this item?'
        cancel='Cancel'
        confirm='Yes, Delete'
        cancelAction={() => state.orderLineItemsCancelDelete(lineItemsStatus.skuToDelete)}
        confirmAction={() => state.deleteLineItem(order, lineItemsStatus.skuToDelete)} />
    </div>
  );
};

export default OrderLineItems;