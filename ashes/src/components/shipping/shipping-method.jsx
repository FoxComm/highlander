import _ from 'lodash';
import React, { PropTypes } from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import ContentBox from '../content-box/content-box';
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
        submitPriceAction={submitPriceAction}
        key={row.id}/>
    );
  };
};

const viewContent = props => {
  const shippingMethods = props.shippingMethods;
  if (shippingMethods !== undefined &&
      shippingMethods[0] !== undefined &&
      shippingMethods[0].name &&
      _.isNumber(shippingMethods[0].price)) {
    return <TableView columns={columns} data={{rows: shippingMethods}} />;
  } else {
    return <div className='fc-content-box__empty-text'>No shipping method applied.</div>;
  }
};

viewContent.propTypes = {
  shippingMethods: PropTypes.array.isRequired
};

const ShippingMethod = props => {
  const availableShippingMethods = props.availableShippingMethods.map(shippingMethod => {
    let isSelected = false;
    if (props.shippingMethods !== undefined &&
        props.shippingMethods.length > 0 &&
        props.shippingMethods[0] !== undefined) {
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

  const ShippingMethodContentBox = props.readOnly ? ContentBox : EditableContentBox;

  return (
    <ShippingMethodContentBox
      className='fc-shipping-methods'
      id={props.id}
      title={props.title}
      isEditing={props.isEditing}
      editAction={props.editAction}
      editButtonId="shipping-method-edit-btn"
      doneAction={props.doneAction}
      doneButtonId="shipping-method-done-btn"
      viewContent={viewContent(props)}
      editContent={editContent}
      indentContent={false}
      />
  );
};

ShippingMethod.propTypes = {
  id: PropTypes.string,
  order: PropTypes.object,
  availableShippingMethods: PropTypes.array,
  isEditing: PropTypes.bool.isRequired,
  editAction: PropTypes.func,
  doneAction: PropTypes.func,
  isEditingPrice: PropTypes.bool,
  editPriceAction: PropTypes.func,
  cancelPriceAction: PropTypes.func,
  shippingMethods: PropTypes.array.isRequired,
  currentOrder: PropTypes.object,
  updateAction: PropTypes.func,
  title: PropTypes.node,
  readOnly: PropTypes.bool,
};

ShippingMethod.defaultProps = {
  title: 'Shipping Method',
  readOnly: false,
};

export default ShippingMethod;
