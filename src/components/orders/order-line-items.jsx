import React, { PropTypes } from 'react';

import ConfirmationDialog from '../modal/confirmation-dialog';
import OrderLineItem from './order-line-item';
import TableView from '../table/tableview';
import EditableContentBox from '../content-box/editable-content-box';

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

const OrderLineItems = props => {
  return (
    <EditableContentBox
      className='fc-line-items'
      title='Items'
      isEditing={props.order.lineItems.isEditing}
      editAction={props.orderLineItemsStartEdit}
      doneAction={props.orderLineItemsCancelEdit}
      editContent={renderEditContent(props)}
      editFooter={renderEditFooter(props)}
      viewContent={renderViewContent(props)} />
  );
};

OrderLineItems.propTypes = {
  order: PropTypes.object,
  orderLineItemsStartEdit: PropTypes.func,
  orderLineItemsCancelEdit: PropTypes.func
};

const renderViewContent = props => {
  return <TableView columns={viewModeColumns} data={{rows: props.order.lineItems.items}}/>;
};

renderViewContent.propTypes = {
  order: PropTypes.object
};

const renderEditContent = props => {
  const order = props.order.currentOrder;
  const lineItemsStatus = props.order.lineItems;

  const orderLineItems = lineItemsStatus.items.map((lineItem, idx) => {
    return <OrderLineItem key={`lineItem-${idx}`} item={lineItem} {...props} />;
  });

  // TODO: Re-add the Typeahead after Andrey's refactor is complete.
  return (
    <div>
      <TableView columns={editModeColumns} data={{rows: orderLineItems}} />
      <ConfirmationDialog
        isVisible={lineItemsStatus.isDeleting}
        header='Confirm'
        body='Are you sure you want to delete this item?'
        cancel='Cancel'
        confirm='Yes, Delete'
        cancelAction={() => props.orderLineItemsCancelDelete(lineItemsStatus.skuToDelete)}
        confirmAction={() => props.deleteLineItem(order, lineItemsStatus.skuToDelete)} />
    </div>
  );
};

const renderEditFooter = props => {
  return (
    <div>
      <strong>Add Item</strong>
    </div>
  );
};

renderEditContent.propTypes = {
  orderLineItemsCancelDelete: PropTypes.func,
  deleteLineItem: PropTypes.func
};

export default OrderLineItems;
