import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import PaymentRow from './row';
import DebitCreditDetails from './debit-credit-details';

const GiftCard = props => {
  const { amount, code } = props.paymentMethod;

  const params = {
    details: <DebitCreditDetails {...props} />,
    amount: amount,
    ...props,
  };

  return <PaymentRow {...params} />;
};

GiftCard.propTypes = {
  paymentMethod: PropTypes.shape({
    amount: PropTypes.number.isRequired,
    availableBalance: PropTypes.number.isRequired,
    code: PropTypes.string.isRequired
  }),
  order: PropTypes.shape({
    referenceNumber: PropTypes.string.isRequired
  }),
};

export default GiftCard;
