/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';

// styles
import styles from './row.css';

// components
import Currency from '../../common/currency';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import PaymentMethod from '../../payment/payment-method';

//types
type Props = {
  method: string;
  state: string;
  shipmentDate: string;
  carrier: string;
  estimatedArrival: string;
  deliveredDate: string;
  trackingNumber: string;
  address: Object;
  lineItems: Array<Object>;
  transactions: Object;
}

type State = {
  isExpanded: boolean;
}


export default class ShipmentRow extends Component {
  props: Props;

  state: State = {
    isExpanded: false,
  };

  @autobind
  toggleExpanded() {
    this.setState({isExpanded: !this.state.isExpanded});
  }

  get summaryRow() {
    const { props, state } = this;
    const toggleAction = state.isExpanded ? 'up' : 'down';

    return (
      <TableRow styleName="summary-row">
        <TableCell>
          <i styleName="row-toggle"
             className={`icon-chevron-${toggleAction}`}
             onClick={this.toggleExpanded}
          />
          {props.method}
        </TableCell>
        <TableCell>
          {props.state}
        </TableCell>
        <TableCell>
          {props.lineItems.length}
        </TableCell>
        <TableCell>
          <DateTime value={props.shipmentDate} />
        </TableCell>
        <TableCell>
          {props.carrier}
        </TableCell>
        <TableCell>
          <DateTime value={props.estimatedArrival} />
        </TableCell>
        <TableCell>
          <DateTime value={props.deliveredDate} />
        </TableCell>
        <TableCell>
          {props.trackingNumber}
        </TableCell>
      </TableRow>
    );
  }

  render() {
    return (
      <tbody>
        {this.summaryRow}
      </tbody>
    );
  }
};
