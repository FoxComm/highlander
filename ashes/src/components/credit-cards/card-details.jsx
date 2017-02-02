import React, { PropTypes } from 'react';
import AddressDetails from '../addresses/address-details';
import PaymentMethod from '../payment/payment-method';

export default class CreditCardDetails extends React.Component {

  static propTypes = {
    card: PropTypes.object
  };

  render() {
    const card = this.props.card;

    return (
      <div className="fc-credit-card">
        <div>
          <PaymentMethod paymentMethod={card} type="creditCard" />
        </div>
        <dl>
          <dt className="fc-credit-card__label">Name on Card</dt>
          <dd className="fc-credit-card__content fct-card-holder-name">{card.holderName}</dd>
        </dl>
        <dl>
          <dt className="fc-credit-card__label">Billing Address</dt>
          <dd className="fc-credit-card__content">
            <AddressDetails address={card.address} />
          </dd>
        </dl>
      </div>
    );
  }
}
