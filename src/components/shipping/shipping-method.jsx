'use strict';

import React from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import TableView from '../table/tableview';

const columns = [
  {field: 'name', text: 'Method'},
  {field: 'price', text: 'Price', type: 'currency'}
];

const ShippingMethod = (props) => {
  const editContent = (
    <TableView columns={columns} data={{rows: props.availableShippingMethods}} setState={()=>{}} />
  );

  const viewContent = (
    <TableView columns={columns} data={{rows: props.shippingMethods}} setState={()=>{}} />
  );

  return (
    <EditableContentBox
      className='fc-shipping-method'
      title='Shipping Method'
      isEditing={props.isEditing}
      editAction={props.editAction}
      doneAction={props.doneAction}
      viewContent={viewContent}
      editContent={editContent}
      />
  );
};

export default ShippingMethod;
