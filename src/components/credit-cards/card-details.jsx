import React, { PropTypes } from 'react';
import AddressDetails from '../addresses/address-details';
import PaymentMethod from '../payment/payment-method';

export default class CreditCardDetails extends React.Component {

  static propTypes = {
    card: PropTypes.object
  };

  constructor(props, context) {
    super(props, context);
  }

  render() {
    const card = this.props.card;

    return (
      <div>
        <div>
          <PaymentMethod card={card} />
        </div>
        <dl>
          <dt>Name on Card</dt>
          <dd>{card.holderName}</dd>
        </dl>
        <dl>
          <dt>Billing Address</dt>
          <dd>
            <AddressDetails address={card.address} />
          </dd>
        </dl>
      </div>
    );
  }
}
