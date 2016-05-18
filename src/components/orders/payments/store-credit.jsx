import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import PaymentRow from './row';
import DebitCreditDetails from './debit-credit-details';

import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

let StoreCreditDetails = props => {
  return (
    <DebitCreditDetails
      {...props}
      saveAction={props.editOrderStoreCreditPayment}
    />
  );
};
StoreCreditDetails = connect(null, PaymentMethodActions)(StoreCreditDetails);

const StoreCredit = props => {
  const { amount } = props.paymentMethod;

  const orderRefNum = props.order.referenceNumber;

  const deletePayment = () => {
    props.deleteOrderStoreCreditPayment(orderRefNum);
  };

  const details = editProps => {
    return <StoreCreditDetails {...props} {...editProps} />;
  };

  const params = {
    details,
    amount,
    deleteAction: deletePayment,
    ...props,
  };

  return <PaymentRow {...params} />;
};

StoreCredit.propTypes = {
  paymentMethod: PropTypes.shape({
    amount: PropTypes.number.isRequired,
    availableBalance: PropTypes.number.isRequired
  }),
  order: PropTypes.shape({
    referenceNumber: PropTypes.string.isRequired
  }),
  isEditing: PropTypes.bool.isRequired,
  deleteOrderStoreCreditPayment: PropTypes.func.isRequired,
  editOrderStoreCreditPayment: PropTypes.func.isRequired,
};

export default StoreCredit;
