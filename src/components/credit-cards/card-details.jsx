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
    let card = this.props.card;

    let paymentMethod = {
      cardType: 'visa',
      cardExp: `${card.expMonth}/${card.expYear}`,
      cardNumber: `xxxx-xxxx-xxxx-${card.lastFour}`
    };

    // @TODO: get rid of this ad-hoc after
    // this task https://www.pivotaltracker.com/n/projects/1352060/stories/108365902 is solved
    // @TODO2: create GH/pivotal issue for that!
    card = {
      ...card,
      region: {
        countryId: 21
      }
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
                            address={ card } />
          </dd>
        </dl>
      </div>
    );
  }
}
