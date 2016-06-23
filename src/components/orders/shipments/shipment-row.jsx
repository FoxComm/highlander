/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';

// styles
import styles from './shipment-row.css';

// components
import Currency from '../../common/currency';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import { DateTime } from '../../common/datetime';
import AddressDetails from '../../addresses/address-details';
import ShippedItem from './shipped-item';
import Transaction from './transaction';

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
    isExpanded: true,
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
        <TableCell>{props.state}</TableCell>
        <TableCell>{props.lineItems.length}</TableCell>
        <TableCell>
          <DateTime value={props.shipmentDate} />
        </TableCell>
        <TableCell>{props.carrier}</TableCell>
        <TableCell>
          <DateTime value={props.estimatedArrival} />
        </TableCell>
        <TableCell>
          <DateTime value={props.deliveredDate} />
        </TableCell>
        <TableCell>{props.trackingNumber}</TableCell>
      </TableRow>
    );
  }

  get detailsRow() {
    const { props, state } = this;

    if (!state.isExpanded) {
      return null;
    }

    return (
      <TableRow styleName="details-row">
        <TableCell colspan={8}>
          <div styleName="details-title">Shipping Address</div>
          <div styleName="address">
            <AddressDetails styleName="address" address={props.address} />
          </div>
          <div styleName="details-title">Items</div>
          <div styleName="items">
            {this.props.lineItems.map((item, index) => (
              <ShippedItem index={index} {...item} />
            ))}
          </div>
          <div styleName="details-title">Transactions</div>
          <div styleName="items">
            {this.props.transactions.map((item, index) => (
              <Transaction index={index} {...item} />
            ))}
          </div>
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
