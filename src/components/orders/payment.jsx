import _ from 'lodash';
import React, { PropTypes } from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import TableView from '../table/tableview';
import ContentBox from '../content-box/content-box';
import PaymentMethodRow from './payment-method-row';

const viewColumns = [
  {field: 'name', text: 'Method'},
  {field: 'amount', text: 'Amount', type: 'currency'}
];

const editColumns = viewColumns.concat([
  {field: 'edit'}
]);

const viewContent = props => {
  const paymentMethods = props.order.currentOrder.paymentMethods;

  const renderRow = (row, index, isNew) => {
    return <PaymentMethodRow paymentMethod={row} isEditing={false} />;
  };

  if (_.isEmpty(paymentMethods)) {
    return <div className="fc-content-box-empty-text">No payment method applied.</div>;
  } else {
    return (
      <TableView
        columns={viewColumns}
        data={{rows: paymentMethods}}
        renderRow={renderRow}
      />
    );
  }
};

const editContent = props => {
  const paymentMethods = props.order.currentOrder.paymentMethods;

  const renderRow = (row, index, isNew) => {
    return <PaymentMethodRow paymentMethod={row} isEditing={true}/>;
  };

  if (_.isEmpty(paymentMethods)) {
    return <div className="fc-content-box-empty-text">Stuff will go here.</div>;
  } else {
    return (
      <TableView
        columns={editColumns}
        data={{rows: paymentMethods}}
        renderRow={renderRow}
      />
    );
  }
};

const OrderPayment = props => {
  return (
    <EditableContentBox
      className="fc-order-payment"
      title="Payment Method"
      isTable={true}
      editContent={editContent(props)}
      isEditing={props.payments.isEditing}
      editAction={props.orderPaymentMethodStartEdit}
      doneAction={() => console.log('not implemented') }
      viewContent={viewContent(props)}
    />
  );
};

OrderPayment.propTypes = {
  order: PropTypes.shape({
    currentOrder: PropTypes.shape({
      paymentMethods: PropTypes.array
    })
  }).isRequired,
  payments: PropTypes.shape({
    isEditing: PropTypes.bool.isRequired
  })
};

export default OrderPayment;
