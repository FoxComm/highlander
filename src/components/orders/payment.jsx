import _ from 'lodash';
import React, { PropTypes } from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import TableView from '../table/tableview';
import ContentBox from '../content-box/content-box';
import PaymentMethodRow from './payment-method-row';

const columns = [
  {field: 'name', text: 'Method'},
  {field: 'amount', text: 'Amount', type: 'currency'}
];

const viewContent = props => {
  const paymentMethods = props.order.paymentMethods;

  const renderRow = (row, index, isNew) => {
    return <PaymentMethodRow paymentMethod={row}/>;
  };

  if (_.isEmpty(paymentMethods)) {
    return <div className="fc-content-box-empty-text">No payment method applied.</div>;
  } else {
    return (
      <TableView
        columns={columns}
        data={{rows: paymentMethods}}
        renderRow={renderRow}
      />
    );
  }
};

const OrderPayment = props => {
  const order = props.order;

  return (
    <EditableContentBox
      className="fc-order-payment"
      title="Payment Method"
      isTable={true}
      isEditing={false}
      editAction={() => console.log("Not implemented")}
      renderFooter={null}
      viewContent={viewContent(props)}
    />
  );
};

OrderPayment.propTypes = {
  order: PropTypes.object.isRequired
};

export default OrderPayment;
