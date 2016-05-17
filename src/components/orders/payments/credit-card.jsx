import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import CreditCardForm from '../../credit-cards/card-form';
import AddressDetails from '../../addresses/address-details';
import PaymentRow from './row';

import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

let CreditCardDetails = (props) => {
  const card = props.paymentMethod;

  const handleSave = (event, creditCard) => {
    props
      .editCreditCardPayment(props.order.referenceNumber, creditCard, props.customerId)
      .then(props.cancelEditing);
  };

  if (!props.isEditing) {
    return (
      <div>
        <dl>
          <dt>Name on Card</dt>
          <dd>{card.holderName}</dd>
        </dl>
        <dl>
          <dt>Billing Address</dt>
          <AddressDetails address={card.address} />
        </dl>
      </div>
    );
  } else {
    return (
      <CreditCardForm
        card={card}
        customerId={props.customerId}
        isDefaultEnabled={false}
        isNew={false}
        onCancel={props.cancelEditing}
        onSubmit={handleSave}
      />
    );
  }
};
CreditCardDetails = connect(null, PaymentMethodActions)(CreditCardDetails);


const CreditCard = props => {
  const deletePayment = () => {
    props.deleteOrderCreditCardPayment(props.order.referenceNumber);
  };

  const details = editProps => {
    return <CreditCardDetails {...props} {...editProps} />;
  };

  const params = {
    details,
    amount: null,
    deleteAction: deletePayment,
    ...props,
  };

  return <PaymentRow {...params} />;
};

CreditCard.propTypes = {
  paymentMethod: PropTypes.shape({
    brand: PropTypes.string.isRequired,
    holderName: PropTypes.string.isRequired,
    address: PropTypes.object.isRequired,
    expMonth: PropTypes.number.isRequired,
    expYear: PropTypes.number.isRequired,
    lastFour: PropTypes.string.isRequired
  }),
  order: PropTypes.shape({
    referenceNumber: PropTypes.string.isRequired
  }),
  isEditing: PropTypes.bool.isRequired,
  deleteOrderCreditCardPayment: PropTypes.func.isRequired,
  editCreditCardPayment: PropTypes.func.isRequired,
};

export default CreditCard;
