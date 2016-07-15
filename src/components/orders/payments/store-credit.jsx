import React, { PropTypes } from 'react';

import PaymentRow from './row';
import DebitCreditDetails from './debit-credit-details';

const StoreCredit = props => {
  const { amount } = props.paymentMethod;

  const params = {
    details: <DebitCreditDetails {...props} />,
    amount,
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
};

export default StoreCredit;
