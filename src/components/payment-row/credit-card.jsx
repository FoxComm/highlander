/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';

import CreditCardForm from 'components/credit-cards/card-form';
import AddressDetails from 'components/addresses/address-details';

import type { CreditCard, Order } from 'paragons/order';

type Props = {
  customerId: number,
  paymentMethod: CreditCard,
  isEditing: boolean,

  handleCancel: Function,
};

export default class CreditCardDetails extends Component {
  props: Props;

  render(): Element {
    const card = this.props.paymentMethod;
    const { customerId, isEditing, handleCancel } = this.props;

    let details = null;

    if (isEditing) {
      details = (
        <CreditCardForm
          card={card}
          customerId={customerId}
          isDefaultEnabled={false}
          isNew={false}
          onCancel={handleCancel}
          onSubmit={_.noop} />
      );
    } else {
      details = (
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
    }

    return details;
  }
}

