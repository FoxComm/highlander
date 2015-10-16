'use strict';

import React, { PropTypes } from 'react';
import Addresses from '../addresses/addresses';
import AddressDetails from '../addresses/address-details';
import Panel from '../panel/panel';
import OrderStore from '../../stores/orders';

export default class OrderShippingAddress extends React.Component {

  static defaultProps = {
    editMode: false
  }

  static propTypes = {
    editMode: PropTypes.bool
  }

  constructor(props, context) {
    super(props, context);
    this.state = {
      isEditing: false
    };
  }

  onSelectAddress(address) {
    OrderStore.setShippingAddress(this.props.order.referenceNumber, address.id);
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  render() {
    let address = this.props.order.shippingAddress;
    let body = <AddressDetails address={address} />;
    let actions = null;
    let editButton = null;

    if (this.props.editMode) {
      if (this.state.isEditing) {
        body = <Addresses order={this.props.order} onSelectAddress={this.onSelectAddress.bind(this)} />;
        editButton = (
          <div>
            <button className="fc-btn fc-btn-plain icon-chevron-up fc-right" onClick={this.toggleEdit.bind(this)}></button>
            <div className="fc-panel-comment fc-right">Patty’s Pub</div>
          </div>
        );
      } else {
        editButton = (
          <div>
            <button className="fc-btn fc-btn-plain icon-chevron-down fc-right" onClick={this.toggleEdit.bind(this)}>
            </button>
            <div className="fc-panel-comment fc-right">Patty’s Pub</div>
          </div>
        );
      }
    }

    return (
      <Panel className="fc-order-shipping-address"
             title="Shipping Address"
             controls={ editButton }
             enablePaddings={ true }>
        {body}
      </Panel>
    );
  }
}
