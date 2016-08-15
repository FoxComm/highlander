/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { autobind } from 'core-decorators';

// helpers
import { getStore } from 'lib/store-creator';

// styles
import styles from './shipment-row.css';

// components
import Currency from 'components/common/currency';
import TableRow from 'components/table/row';
import TableCell from 'components/table/cell';
import { DateTime } from 'components/common/datetime';
import AddressDetails from 'components/addresses/address-details';
import ShippedItem from './shipped-item';
import Transaction from './transaction';
import WaitAnimation from 'components/common/wait-animation';

//types
import type { Dictionary } from 'paragons/types';
import type { Carrier, ShippingMethod, ShipmentLineItem } from 'paragons/shipment';

type Props = {
  actions: {
    carriers: Dictionary<Function>;
    shippingMethods: Dictionary<Function>;
  };
  carriers: Array<Carrier>;
  shippingMethods: Array<ShippingMethod>;
  isLoading: boolean;
  shippingMethod: ShippingMethod;
  state: string;
  shipmentDate: string;
  estimatedArrival: string;
  deliveredDate: string;
  trackingNumber: string;
  address: Object;
  lineItems: Array<ShipmentLineItem>;
  transactions: Array<Object>; //TODO: will be picked from phoenix
}

type State = {
  isExpanded: boolean;
}

const mapStateToProps = state => {
  const { carriers, shipmentMethods } = state.orders;
  const { fetchCarriers } = carriers;
  const { fetchShippingMethods } = shipmentMethods;
  const carriersLoading = !fetchCarriers.isCompleted || fetchCarriers.isRunning;
  const shippingMethodsLoading = !fetchShippingMethods.isCompleted || fetchShippingMethods.isRunning;

  return {
    carriers: carriers.list,
    shippingMethods: shipmentMethods.list,
    isLoading: carriersLoading || shippingMethodsLoading,
  };
};

const mapDispatchToProps = dispatch => ({
  actions: {
    carriers: bindActionCreators(getStore('orders.carriers').actions, dispatch),
    shippingMethods: bindActionCreators(getStore('orders.shipmentMethods').actions, dispatch),
  },
});

class ShipmentRow extends Component {
  props: Props;

  state: State = {
    isExpanded: false,
  };

  static defaultProps = {
    transactions: [],
  };

  componentDidMount(): void {
    const { carriers, shippingMethods } = this.props.actions;

    carriers.fetchCarriers();
    shippingMethods.fetchShippingMethods();
  }

  @autobind
  toggleExpanded() {
    this.setState({ isExpanded: !this.state.isExpanded });
  }

  get summaryRow() {
    const { props, state } = this;
    const toggleAction = state.isExpanded ? 'up' : 'down';

    const shippingMethod = props.shippingMethods
      .filter(shippingMethod => shippingMethod.id === _.get(props,'shippingMethod.id',null)).pop();
    const carrier = props.carriers
      .filter(carrier => carrier.id === _.get(props, 'shippingMethod.carrier.id', null)).pop();
    const trackingLink = props.trackingNumber ? (
      <a
        href={carrier.trackingTemplate.replace('$number', props.trackingNumber)}
        styleName="tracking-link"
        target="_blank"
      >
        {props.trackingNumber}
      </a>
    ) : null;

    return (
      <TableRow styleName="summary-row">
        <TableCell>
          <i styleName="row-toggle"
             className={`icon-chevron-${toggleAction}`}
             onClick={this.toggleExpanded}
          />
          {shippingMethod.name}
        </TableCell>
        <TableCell>{props.state}</TableCell>
        <TableCell>{props.lineItems.length}</TableCell>
        <TableCell>
          <DateTime value={props.shipmentDate} />
        </TableCell>
        <TableCell>{carrier.name}</TableCell>
        <TableCell>
          <DateTime value={props.estimatedArrival} />
        </TableCell>
        <TableCell>
          <DateTime value={props.deliveredDate} />
        </TableCell>
        <TableCell>{trackingLink}</TableCell>
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
    if (this.props.isLoading) {
      return (
        <tbody>
          <tr>
            <td colSpan={8}>
              <WaitAnimation />
            </td>
          </tr>
        </tbody>
      );
    }

    return (
      <tbody>
        {this.summaryRow}
        {this.detailsRow}
      </tbody>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ShipmentRow);
