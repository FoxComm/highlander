/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

import CreditCardDetails from './credit-card';
import PaymentMethodDetails from 'components/payment/payment-method';
import TableCell from 'components/table/cell';
import TableRow from 'components/table/row';
import { DateTime } from 'components/common/datetime';
import { EditButton } from 'components/common/buttons';

import styles from './payment-row.css';

import type { PaymentMethod } from 'paragons/order';

type Props = {
  customerId: number,
  editMode: boolean,
  orderReferenceNumber: string,
  paymentMethod: PaymentMethod,
};

type State = {
  isEditing: boolean,
  showDetails: boolean,
};

export default class PaymentRow extends Component {
  props: Props;
  state: State = { 
    isEditing: false,
    showDetails: false,
  };

  get amount(): ?Element {
    return null;
  }

  get details(): ?Element {
    if (this.state.showDetails) {
      const { customerId, paymentMethod } = this.props;
      const details = (
        <CreditCardDetails
          customerId={customerId}
          isEditing={this.state.isEditing}
          handleCancel={this.cancelEdit}
          paymentMethod={paymentMethod} />
      );

      return (
        <TableRow styleName="details-row">
          <TableCell colSpan={5}>
            {details}
          </TableCell>
        </TableRow>
      );
    }
  }

  get editActions(): ?Element {
    if (this.props.editMode) {
      const editButton = !this.state.isEditing
        ? <EditButton onClick={this.startEdit} />
        : null;

      return (
        <TableCell styleName="actions-cell">
          {editButton}
        </TableCell>
      );
    }
  }

  get summary(): Element {
    const { paymentMethod } = this.props;
    const dir = this.state.showDetails ? 'up' : 'down';
    const iconClass = `icon-chevron-${dir}`;

    return (
      <TableRow styleName="payment-row">
        <TableCell>
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

  render(): Element {
    return (
      <tbody>
        {this.summary}
        {this.details}
      </tbody>
    );
  }
}

