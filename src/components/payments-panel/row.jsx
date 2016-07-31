/* @flow */

import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';

import styles from './payments.css';

import Currency from 'components/common/currency';
import { DeleteButton, EditButton } from 'components/common/buttons';
import TableRow from 'components/table/row';
import TableCell from 'components/table/cell';
import { DateTime } from 'components/common/datetime';
import PaymentMethod from 'components/payment/payment-method';

type Props = {
  editMode: boolean,
  title: string|Element;
  subtitle?: string|Element;
  amount: ?number;
  details: Element;
  showDetails: boolean;
  toggleDetails: Function;
  deleteAction: Function,
  paymentMethod: Object;
}

type State = {
  isEditing: boolean,
};

export default class PaymentRow extends Component {
  props: Props;
  state: State = { isEditing: false };

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
      const editButton = !this.state.isEditing 
        ? <EditButton onClick={this.handleStartEdit} />
        : null;

      return (
        <TableCell styleName="actions-cell">
          {editButton}
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
      const detailsProps = { ...this.props, isEditing: this.state.isEditing };
      const details = React.cloneElement(this.props.details, detailsProps);

      return (
        <TableRow styleName="details-row">
          <TableCell colSpan={5}>
            {details}
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
        <TableCell />
        <TableCell>
          <DateTime value={props.paymentMethod.createdAt} />
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
