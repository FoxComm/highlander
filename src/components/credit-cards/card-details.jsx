import React, { PropTypes } from 'react';
import AddressDetails from '../addresses/address-details';
import PaymentMethod from '../payment/payment-method';

export default class CreditCardDetails extends React.Component {

  static propTypes = {
    card: PropTypes.object,
    customerId: PropTypes.number.isRequired,
  };

  constructor(props, context) {
    super(props, context);
  }

  render() {
    const card = this.props.card;

    let paymentMethod = {
      cardType: 'visa',
      cardExp: `${card.expMonth}/${card.expYear}`,
      cardNumber: `xxxx-xxxx-xxxx-${card.lastFour}`
    };

    return (
      <div>
        <div>
          <PaymentMethod model={ paymentMethod } />
        </div>
        <dl>
          <dt>Name on Card</dt>
          <dd>{ card.holderName }</dd>
        </dl>
        <dl>
          <dt>Billing Address</dt>
          <dd>
            <AddressDetails customerId={ this.props.customerId }
                            address={ card.address } />
          </dd>
        </dl>
      </div>
    );
  }
}
