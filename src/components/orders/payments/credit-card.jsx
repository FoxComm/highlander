import React, { PropTypes } from 'react';
import CreditCardDetails from '../../../components/credit-cards/card-details';
import AddressDetails from '../../addresses/address-details';
import PaymentRow from './row';

const CreditCard = props => {
  const card = props.paymentMethod;

  const deletePayment = () => {
    props.deleteOrderCreditCardPayment(props.order.currentOrder.refNum);
  };

  const details = (
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

  const params = {
    details: details,
    amount: null,
    deleteAction: deletePayment,
    ...props,
  };

  return PaymentRow(params);
};

CreditCard.propTypes = {
  paymentMethod: PropTypes.shape({
    paymentMethod: PropTypes.shape({
      brand: PropTypes.string.isRequired,
      holderName: PropTypes.string.isRequired,
      address: PropTypes.object.isRequired,
      expMonth: PropTypes.number.isRequired,
      expYear: PropTypes.number.isRequired,
      lastFour: PropTypes.string.isRequired
    })
  }),
  order: PropTypes.shape({
    currentOrder: PropTypes.shape({
      referenceNumber: PropTypes.string.isRequired
    })
  }),
  isEditing: PropTypes.bool.isRequired,
  deleteOrderCreditCardPayment: PropTypes.func.isRequired
};

export default CreditCard;
