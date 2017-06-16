/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import TableRow from 'components/table/row';
import TableCell from 'components/table/cell';
import { DateTime } from 'components/common/datetime';
import AddressDetails from 'components/addresses/address-details';
import ShippedItem from './shipped-item';
import Transaction from './transaction';
import Icon from 'components/core/icon';

// styles
import styles from './shipment-row.css';

// types
import type { TShippingMethod, TShipmentLineItem } from 'paragons/shipment';

type Props = {
  isLoading: boolean;
  shippingMethod: TShippingMethod;
  state: string;
  shipmentDate: string;
  estimatedArrival: string;
  deliveredDate: string;
  trackingNumber: string;
  address: Object;
  lineItems: Array<TShipmentLineItem>;
  transactions: Array<Object>; //TODO: will be picked from phoenix
}

type State = {
  isExpanded: boolean;
}

export default class ShipmentRow extends Component {
  props: Props;

  state: State = {
    isExpanded: false,
  };

  static defaultProps = {
    transactions: [],
  };

  @autobind
  toggleExpanded() {
    this.setState({ isExpanded: !this.state.isExpanded });
  }

  get trackingLink() {
    const { shippingMethod, trackingNumber } = this.props;

    if (!trackingNumber) {
      return null;
    }

    return (
      <a
        href={shippingMethod.carrier.trackingTemplate.replace('$number', trackingNumber)}
        styleName="tracking-link"
        target="_blank"
      >
        {trackingNumber}
      </a>
    );
  }

  get summaryRow() {
    const { props, state } = this;
    const toggleAction = state.isExpanded ? 'up' : 'down';

    return (
      <TableRow styleName="summary-row">
        <TableCell>
          <Icon styleName="row-toggle"
             name={`chevron-${toggleAction}`}
             onClick={this.toggleExpanded}
          />
          {props.shippingMethod.name}
        </TableCell>
        <TableCell>{props.state}</TableCell>
        <TableCell>{props.lineItems.length}</TableCell>
        <TableCell>
          <DateTime value={props.shipmentDate} />
        </TableCell>
        <TableCell>{props.shippingMethod.carrier.name}</TableCell>
        <TableCell>
          <DateTime value={props.estimatedArrival} />
        </TableCell>
        <TableCell>
          <DateTime value={props.deliveredDate} />
        </TableCell>
        <TableCell>{this.trackingLink}</TableCell>
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
        <TableCell colSpan={8}>
          <div styleName="details-title">Shipping Address</div>
          <div styleName="address">
            <AddressDetails styleName="address" address={props.address} />
          </div>
          <div styleName="details-title">Items</div>
          <div styleName="items">
            {this.props.lineItems.map((item, index) => (
              <ShippedItem key={index} {...item} />
            ))}
          </div>
          <div styleName="details-title">Transactions</div>
          <div styleName="items">
            {this.props.transactions.map((item, index) => (
              <Transaction key={index} {...item} />
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
}
