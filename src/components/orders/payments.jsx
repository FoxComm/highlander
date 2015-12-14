import _ from 'lodash';
import React, { PropTypes } from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import TableView from '../table/tableview';
import ContentBox from '../content-box/content-box';
import GiftCard from './payments/gift-card';
import StoreCredit from './payments/store-credit';
import CreditCard from './payments/credit-card';
import Dropdown from '../dropdown/dropdown';
import { AddButton } from '../common/buttons';

const viewColumns = [
  {field: 'name', text: 'Method'},
  {field: 'amount', text: 'Amount', type: 'currency'}
];

const editColumns = viewColumns.concat([
  {field: 'edit'}
]);

const renderRow = (isEditing, props) => {
  return (row, index, isNew) => {
    switch(row.type) {
      case 'giftCard':
        return <GiftCard paymentMethod={row} isEditing={isEditing} {...props} />;
      case 'creditCard':
        return <CreditCard paymentMethod={row} isEditing={isEditing} {...props} />;
      case 'storeCredit':
        return <StoreCredit paymentMethod={row} isEditing={isEditing} {...props} />;
    }
  };
};

const viewContent = props => {
  const paymentMethods = props.order.currentOrder.paymentMethods;

  if (_.isEmpty(paymentMethods)) {
    return <div className="fc-content-box__empty-text">No payment method applied.</div>;
  } else {
    return (
      <TableView
        columns={viewColumns}
        data={{rows: paymentMethods}}
        renderRow={renderRow(false, props)}
      />
    );
  }
};

viewContent.propTypes = {
  order: PropTypes.shape({
    currentOrder: PropTypes.shape({
      paymentMethods: PropTypes.array
    })
  }).isRequired
};

const editContent = props => {
  const paymentMethods = props.order.currentOrder.paymentMethods;

  const paymentTypes = {
    giftCard: 'Gift Card',
    creditCard: 'Credit Card',
    storeCredit: 'Store Credit'
  };

  return (
    <TableView
      columns={editColumns}
      data={{rows: paymentMethods}}
      renderRow={renderRow(true, props)}
    />
  );
};

editContent.propTypes = {
  order: PropTypes.shape({
    currentOrder: PropTypes.shape({
      paymentMethods: PropTypes.array
    })
  }).isRequired
};

const Payments = props => {
  return (
    <EditableContentBox
      className="fc-order-payment"
      title="Payment Method"
      isTable={true}
      editContent={editContent(props)}
      isEditing={props.payments.isEditing}
      editAction={props.orderPaymentMethodStartEdit}
      doneAction={props.orderPaymentMethodStopEdit}
      viewContent={viewContent(props)}
    />
  );
};

Payments.propTypes = {
  order: PropTypes.shape({
    currentOrder: PropTypes.shape({
      paymentMethods: PropTypes.array
    })
  }).isRequired,
  payments: PropTypes.shape({
    isEditing: PropTypes.bool.isRequired
  }),
  orderPaymentMethodStartEdit: PropTypes.func.isRequired,
  orderPaymentMethodStopEdit: PropTypes.func.isRequired,

  deleteOrderGiftCardPayment: PropTypes.func.isRequired,
  deleteOrderStoreCreditPayment: PropTypes.func.isRequired,
  deleteOrderCreditCardPayment: PropTypes.func.isRequired
};

export default Payments;
