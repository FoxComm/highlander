import React, { PropTypes } from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import TableView from '../table/tableview';
import ShippingMethodRow from './shipping-method-row';

const columns = [
  {field: 'name', text: 'Method'},
  {field: 'price', text: 'Price', type: 'currency'}
];

const renderRowFn = (order, updateAction, isEditingPrice, editPriceAction, cancelPriceAction, submitPriceAction) => {
  return (row, index, isNew) => {
    const isSelected = row.isSelected;
    return (
      <ShippingMethodRow 
        shippingMethod={row}
        updateAction={isSelected ? ()=>{} : () => updateAction(order, row)} 
        isEditingPrice={isEditingPrice}
        editPriceAction={editPriceAction}
        cancelPriceAction={cancelPriceAction}
        submitPriceAction={submitPriceAction} />
    );
  };
};


const ShippingMethod = props => {
  const availableShippingMethods = props.availableShippingMethods.map(shippingMethod => {
    let isSelected = false;
    if (props.shippingMethods && props.shippingMethods.length > 0) {
      isSelected = props.shippingMethods[0].id == shippingMethod.id;
    }

    return {
      ...shippingMethod,
      isSelected: isSelected
    };
  });

  const renderRow = renderRowFn(
      props.currentOrder,
      props.updateAction, 
      props.isEditingPrice, 
      props.editPriceAction, 
      props.cancelPriceAction);

  const editContent = (
    <TableView 
      columns={columns} 
      data={{rows: availableShippingMethods}}
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
  order: PropTypes.object,
  availableShippingMethods: PropTypes.array,
  isEditing: PropTypes.bool.isRequired,
  editAction: PropTypes.func,
  doneAction: PropTypes.func,
  isEditingPrice: PropTypes.bool,
  editPriceAction: PropTypes.func,
  cancelPriceAction: PropTypes.func,
  shippingMethods: PropTypes.array.isRequired
};

export default ShippingMethod;
