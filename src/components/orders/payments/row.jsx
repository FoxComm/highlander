/* @flow */

import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';

import styles from './payments.css';

import Currency from '../../common/currency';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import { EditButton, DeleteButton } from '../../common/buttons';
import PaymentMethod from '../../payment/payment-method';

type Props = {
  editMode: boolean;
  title: string|Element;
  subtitle?: string|Element;
  amount: number;
  details: (editProps: Object) => Element;
  showDetails: boolean;
  toggleDetails: Function;
  editAction: Function;
  deleteAction: Function;
  paymentMethod: Object;
}

type State = {
  isEditing: boolean;
}

export default class PaymentRow extends Component {
  props: Props;

  state: State = {
    isEditing: false,
  };

  @autobind
  handleStartEdit() {
    this.setState({
      isEditing: true,
    });
    if (!this.props.showDetails) {
      this.props.toggleDetails();
    }
  }

  @autobind
  cancelEditing() {
    this.setState({
      isEditing: false,
    });
  }

  get editAction() {
    if (this.props.editMode) {
      return (
        <TableCell>
          <EditButton onClick={this.handleStartEdit} />
          <DeleteButton onClick={this.props.deleteAction} />
        </TableCell>
      );
    }
  }

  get amount() {
    const { amount } = this.props;
    return _.isNumber(amount) ? <Currency value={amount} /> : null;
  }

  get detailsRow() {
    if (this.props.showDetails) {
      const detailsProps = {
        isEditing: this.state.isEditing,
        cancelEditing: this.cancelEditing,
      };
      return (
        <TableRow styleName="details-row">
          <TableCell colspan={5}>
            {this.props.details(detailsProps)}
          </TableCell>
        </TableRow>
      );
    }
  }

  get summaryRow() {
    const { props } = this;
    const nextDetailAction = props.showDetails ? 'up' : 'down';

    return (
      <TableRow styleName="payment-row">
        <TableCell>
          <i styleName="row-toggle" className={`icon-chevron-${nextDetailAction}`} onClick={props.toggleDetails} />
          <PaymentMethod paymentMethod={props.paymentMethod} />
        </TableCell>
        <TableCell>
          {this.amount}
        </TableCell>
        <TableCell></TableCell>
        <TableCell>
          <DateTime value={props.paymentMethod.createdAt}></DateTime>
        </TableCell>
        {this.editAction}
      </TableRow>
    );
  }

  render() {
    return (
      <tbody>
        {this.summaryRow}
        {this.detailsRow}
      </tbody>
    );
  }
};
