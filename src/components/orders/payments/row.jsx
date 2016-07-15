/* @flow */

import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';

import styles from './payments.css';

import Currency from 'components/common/currency';
import TableRow from 'components/table/row';
import TableCell from 'components/table/cell';
import { DateTime } from 'components/common/datetime';
import PaymentMethod from 'components/payment/payment-method';

type Props = {
  title: string|Element;
  subtitle?: string|Element;
  amount: number;
  details: Element;
  showDetails: boolean;
  toggleDetails: Function;
  paymentMethod: Object;
}

export default class PaymentRow extends Component {
  props: Props;

  get amount() {
    const { amount } = this.props;
    return _.isNumber(amount) ? <Currency value={amount} /> : null;
  }

  get detailsRow() {
    if (this.props.showDetails) {
      return (
        <TableRow styleName="details-row">
          <TableCell colSpan={5}>
            {this.props.details}
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
