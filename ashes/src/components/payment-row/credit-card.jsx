/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import CreditCardForm from 'components/credit-cards/card-form';
import AddressDetails from 'components/addresses/address-details';

import * as PaymentMethodActions from 'modules/carts/payment-methods';

import type { CreditCard, Order } from 'paragons/order';

type Props = {
  customerId: number,
  orderReferenceNumber: string,
  paymentMethod: CreditCard,
  isEditing: boolean,
  handleCancel: Function,
  editCreditCardPayment: Function,
};

export class CreditCardDetails extends Component {
  props: Props;

  @autobind
  saveCreditCard(event: Object, card: CreditCard) {
    const { customerId, orderReferenceNumber } = this.props;
    this.props.editCreditCardPayment(orderReferenceNumber, card, customerId)
      .then(this.props.handleCancel);
  }

  render() {
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
          onSubmit={this.saveCreditCard} />
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

export default connect(null, PaymentMethodActions)(CreditCardDetails);
