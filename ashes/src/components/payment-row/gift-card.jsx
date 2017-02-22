/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import DebitCreditDetails from './debit-credit-details';
import * as CartDetails from 'modules/carts/details';

type Props = {
  orderReferenceNumber: string,
  paymentMethod: {
    amount: number,
    code: string,
    availableBalance: number,
  },
  isEditing: boolean,
  handleCancel: Function,
  editGiftCardPayment: Function,
};

export class GiftCardDetails extends Component {
  props: Props;

  @autobind
  saveAction(orderReferenceNumber: string, amount: number): Promise<*> {
    const { code } = this.props.paymentMethod;
    return this.props.editGiftCardPayment(orderReferenceNumber, code, amount);
  }

  render() {
    const { isEditing, handleCancel, orderReferenceNumber, paymentMethod } = this.props;
    return (
      <DebitCreditDetails
        orderReferenceNumber={orderReferenceNumber}
        paymentMethod={paymentMethod}
        isEditing={isEditing}
        handleCancel={handleCancel}
        saveAction={this.saveAction} />
    );
  }
}

export default connect(null, CartDetails)(GiftCardDetails);
