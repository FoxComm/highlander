import React, { PropTypes } from 'react';
import Currency from '../../common/currency';
import PaymentRow from './row';

const StoreCredit = props => {
  const { amount, availableBalance } = props.paymentMethod;

  const orderRefNum = props.order.currentOrder.referenceNumber;
  const futureBalance = availableBalance - amount;

  const deletePayment = () => {
    props.deleteOrderStoreCreditPayment(orderRefNum);
  };

  const details = (
    <div>
      <dl>
        <dt>Available Balance</dt>
        <dd><Currency value={availableBalance} /></dd>
      </dl>
      <dl>
        <dt>Future Available Balance</dt>
        <dd><Currency value={futureBalance} /></dd>
      </dl>
    </div>
  );

  const params = {
    details: details,
    amount: amount,
    deleteAction: deletePayment,
    ...props,
  };

  return <PaymentRow {...params} />;
};

StoreCredit.propTypes = {
  paymentMethod: PropTypes.shape({
    paymentMethod: PropTypes.shape({
      amount: PropTypes.number.isRequired,
      availableBalance: PropTypes.number.isRequired
    })
  }),
  order: PropTypes.shape({
    currentOrder: PropTypes.shape({
      referenceNumber: PropTypes.string.isRequired
    })
  }),
  isEditing: PropTypes.bool.isRequired,
  deleteOrderStoreCreditPayment: PropTypes.func.isRequired
};

export default StoreCredit;
