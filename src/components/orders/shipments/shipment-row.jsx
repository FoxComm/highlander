/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { autobind } from 'core-decorators';

// helpers
import { getStore } from '../../../lib/store-creator';

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
import WaitAnimation from '../../common/wait-animation';

//types
type Props = {
  actions: {
    carriers: { [key: string]: Function};
    shipmentMethods: { [key: string]: Function};
  };
  carriers: Array<Object>;
  shipmentMethods: Array<Object>;
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
  isLoading: boolean;
  isExpanded: boolean;
}


const mapStateToProps = state => ({
  carriers: state.orders.carriers.list,
  shipmentMethods: state.orders.shipmentMethods.list,
});
const mapDispatchToProps = dispatch => ({
  actions: {
    carriers: bindActionCreators(getStore('orders.carriers').actions, dispatch),
    shipmentMethods: bindActionCreators(getStore('orders.shipmentMethods').actions, dispatch),
  },
});


class ShipmentRow extends Component {
  props: Props;

  state: State = {
    isLoading: true,
    isExpanded: false,
  };

  componentDidMount(): void {
    const { carriers, shipmentMethods } = this.props.actions;

    this.setState({isLoading: true});

    Promise.all([carriers.load(), shipmentMethods.load()])
      .then(() => this.setState({isLoading: false}));
  }

  @autobind
  toggleExpanded() {
    this.setState({isExpanded: !this.state.isExpanded});
  }

  get summaryRow() {
    const { props, state } = this;
    const toggleAction = state.isExpanded ? 'up' : 'down';

    const method = props.shipmentMethods.filter(method => method.id === props.method).pop();
    const carrier = props.carriers.filter(carrier => carrier.id === props.carrier).pop();
    const trackingLink = (
      <a href={carrier.trackingTemplate.replace('$number', props.trackingNumber)} target="_blank">
        {props.trackingNumber}
      </a>
    );

    return (
      <TableRow styleName="summary-row">
        <TableCell>
          <i styleName="row-toggle"
             className={`icon-chevron-${toggleAction}`}
             onClick={this.toggleExpanded}
          />
          {method.name}
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
        <TableCell colspan={8}>
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
    if (this.state.isLoading) {
      return (
        <tbody>
          <tr>
            <td colspan={8}>
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
};

export default connect(mapStateToProps, mapDispatchToProps)(ShipmentRow);
