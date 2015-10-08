'use strict';

import React, { PropTypes } from 'react';
import AddressDetails from '../addresses/address-details';

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

    return (
      <div>
        <div>
        Card type
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
