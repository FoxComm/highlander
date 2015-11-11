import React, { PropTypes } from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import TableView from '../table/tableview';
import ShippingMethodRow from './shipping-method-row';

const columns = [
  {field: 'name', text: 'Method'},
  {field: 'price', text: 'Price', type: 'currency'}
];

const renderRow = (row, index, isNew) => {
  return <ShippingMethodRow shippingMethod={row} isSelected={false} onSelect={()=>{}} />;
};


const ShippingMethod = props => {
  const editContent = (
    <TableView 
      columns={columns} 
      data={{rows: props.availableShippingMethods}}
      renderRow={renderRow}
      />
  );

  const viewContent = (
    <TableView columns={columns} data={{rows: props.shippingMethods}} />
  );

  return (
    <EditableContentBox
      className='fc-shipping-methods'
      title='Shipping Method'
      isEditing={props.isEditing}
      editAction={props.editAction}
      doneAction={props.doneAction}
      viewContent={viewContent}
      editContent={editContent}
      />
  );
};

ShippingMethod.propTypes = {
  shippingMethods: PropTypes.array
};

export default ShippingMethod;
