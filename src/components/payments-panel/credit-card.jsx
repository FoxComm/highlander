import _ from 'lodash';
import React, { PropTypes } from 'react';

import CreditCardForm from 'components/credit-cards/card-form';
import AddressDetails from 'components/addresses/address-details';
import PaymentRow from './row';

const CreditCard = props => {
  // TODO: Push this into a separate component.
  const card = props.paymentMethod;
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
    details,
    amount: null,
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
};

export default CreditCard;
