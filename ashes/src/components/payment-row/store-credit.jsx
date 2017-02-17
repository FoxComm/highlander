/* @flow */
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import DebitCreditDetails from './debit-credit-details';
import * as CartDetails from 'modules/carts/details';

type Props = {
  orderReferenceNumber: string,
  paymentMethod: {
    amount: number,
    availableBalance: number,
  },
  isEditing: boolean,
  handleCancel: Function,
  setStoreCreditPayment: Function,
};

export class StoreCreditDetails extends Component {
  props: Props;

  render() {
    const { isEditing, orderReferenceNumber, paymentMethod } = this.props;
    return (
      <DebitCreditDetails
        orderReferenceNumber={orderReferenceNumber}
        paymentMethod={paymentMethod}
        isEditing={isEditing}
        handleCancel={this.props.handleCancel}
        saveAction={this.props.setStoreCreditPayment} />
    );
  }
}

export default connect(null, CartDetails)(StoreCreditDetails);
