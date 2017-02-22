/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { connect } from 'react-redux';

import CreditCardDetails from './credit-card';
import Currency from 'components/common/currency';
import GiftCardDetails from './gift-card';
import StoreCreditDetails from './store-credit';
import PaymentMethodDetails from 'components/payment/payment-method';
import TableCell from 'components/table/cell';
import TableRow from 'components/table/row';
import { DateTime } from 'components/common/datetime';
import { EditButton, DeleteButton } from 'components/common/buttons';

import styles from './payment-row.css';
import {deleteCreditCardPayment, deleteGiftCardPayment, deleteStoreCreditPayment} from 'modules/carts/details';

import type { PaymentMethod } from 'paragons/order';

type Props = {
  customerId: number,
  editMode: boolean,
  orderReferenceNumber: string,
  paymentMethod: PaymentMethod,
  deleteCreditCardPayment: (refNum: string) => Promise<*>,
  deleteGiftCardPayment: (refNum: string, code: string) => Promise<*>,
  deleteStoreCreditPayment: (refNum: string) => Promise<*>,
};

type State = {
  isEditing: boolean,
  showDetails: boolean,
};

class PaymentRow extends Component {
  props: Props;
  state: State = {
    isEditing: false,
    showDetails: false,
  };

  @autobind
  deletePayment(): ?Promise<*> {
    const { orderReferenceNumber, paymentMethod } = this.props;

    switch (paymentMethod.type) {
      case 'creditCard':
        return this.props.deleteCreditCardPayment(orderReferenceNumber);
      case 'giftCard':
        return this.props.deleteGiftCardPayment(orderReferenceNumber, paymentMethod.code);
      case 'storeCredit':
        return this.props.deleteStoreCreditPayment(orderReferenceNumber);
    }
  }

  get amount(): ?Element<*> {
    const amount = _.get(this.props, 'paymentMethod.amount');
    return _.isNumber(amount) ? <Currency value={amount} /> : null;
  }

  get details(): ?Element<*> {
    if (this.state.showDetails) {
      const { customerId, orderReferenceNumber, paymentMethod } = this.props;
      const detailsProps = {
        customerId,
        orderReferenceNumber,
        paymentMethod,
        isEditing: this.state.isEditing,
        handleCancel: this.cancelEdit,
      };

      let DetailsElement = null;
      switch(paymentMethod.type) {
        case 'creditCard':
          DetailsElement = CreditCardDetails;
          break;
        case 'giftCard':
          DetailsElement = GiftCardDetails;
          break;
        case 'storeCredit':
          DetailsElement = StoreCreditDetails;
          break;
      }

      if (DetailsElement == null) {
        throw `Unexpected payment method ${paymentMethod.type}`;
      }

      return (
        <TableRow key="details" styleName="details-row">
          <TableCell colSpan={5}>
            <DetailsElement {...detailsProps} />
          </TableCell>
        </TableRow>
      );
    }
  }

  get editActions(): ?Element<*> {
    if (this.props.editMode) {
      const editButton = !this.state.isEditing
        ? <EditButton onClick={this.startEdit} />
        : null;

      return (
        <TableCell styleName="actions-cell">
          {editButton}
          <DeleteButton onClick={this.deletePayment} />
        </TableCell>
      );
    }
  }

  get summary(): Element<*> {
    const { paymentMethod } = this.props;
    const dir = this.state.showDetails ? 'up' : 'down';
    const iconClass = `icon-chevron-${dir}`;

    return (
      <TableRow key="summary" styleName="payment-row">
        <TableCell styleName="toggle-column">
          <i styleName="row-toggle" className={iconClass} onClick={this.toggleDetails} />
          <PaymentMethodDetails paymentMethod={paymentMethod} />
        </TableCell>
        <TableCell>
          {this.amount}
        </TableCell>
        <TableCell />
        <TableCell>
          <DateTime value={paymentMethod.createdAt} />
        </TableCell>
        {this.editActions}
      </TableRow>
    );
  }

  @autobind
  startEdit() {
    this.setState({
      isEditing: true,
      showDetails: true,
    });
  }

  @autobind
  cancelEdit() {
    this.setState({
      isEditing: false,
      showDetails: false,
    });
  }

  @autobind
  toggleDetails() {
    this.setState({
      showDetails: !this.state.showDetails
    });
  }

  render() {
    return (
      <tbody>
        {this.summary}
        {this.details}
      </tbody>
    );
  }
}

const deleteActions = {
  deleteCreditCardPayment, deleteGiftCardPayment, deleteStoreCreditPayment
};

export default connect(void 0, deleteActions)(PaymentRow);

