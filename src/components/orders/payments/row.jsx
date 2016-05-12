/* @flow */

import _ from 'lodash';
import React, { PropTypes, Element } from 'react';
import static_url from '../../../lib/s3';

import styles from './payments.css';

import Currency from '../../common/currency';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import { EditButton, DeleteButton } from '../../common/buttons';

type Props = {
  isEditing: boolean;
  type: string;
  title: string|Element;
  subtitle?: string|Element;
  amount: number;
  details: Element;
  showDetails: boolean;
  even: boolean;
  toggleDetails: Function;
  editAction: Function;
  deleteAction: Function;
  paymentMethod: Object;
}

const PaymentRow = (props: Props) => {
  const editAction = props.isEditing ? (
      <TableCell>
        <EditButton onClick={props.editAction} />
        <DeleteButton onClick={props.deleteAction} />
      </TableCell>
    ) : null;
  const trClass = props.even ? 'even' : null;

  const amount = _.isNumber(props.amount) ? <Currency value={props.amount} /> : null;
  const detailsRow = props.showDetails ? (
    <TableRow className={trClass} styleName="details-row">
      <TableCell colspan={5}>
        {props.details}
      </TableCell>
    </TableRow>
  ) : null;

  const { type, title, subtitle } = props;
  const nextDetailAction = props.showDetails ? 'up' : 'down';
  const icon = static_url(`images/payments/payment_${type}.svg`);

  return [
    <TableRow className={trClass} styleName="payment-row">
      <TableCell>
        <i styleName="row-toggle" className={`icon-chevron-${nextDetailAction}`} onClick={props.toggleDetails} />
        <img styleName="payment-icon" className="fc-icon-lg" src={icon} />
        <div styleName="payment-summary">
          <strong>{title}</strong>
          <div>{subtitle}</div>
        </div>
      </TableCell>
      <TableCell>
        {amount}
      </TableCell>
      <TableCell></TableCell>
      <TableCell>
        <DateTime value={props.paymentMethod.createdAt}></DateTime>
      </TableCell>
      {editAction}
    </TableRow>,
    detailsRow
  ];
};

export default PaymentRow;
