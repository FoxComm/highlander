'use strict';

import React, { PropTypes } from 'react';
import Addresses from '../addresses/addresses';
import AddressDetails from '../addresses/address-details';
import Panel from '../panel/panel';
import OrderStore from '../../stores/orders';

export default class OrderShippingAddress extends React.Component {

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
    let body = null;
    let editButton = null;
    let footer = null;

    if (this.state.isEditing) {
      body = <Addresses order={this.props.order} onSelectAddress={this.onSelectAddress.bind(this)} />;
      footer = (
        <footer className="fc-line-items-footer">
          <div>
            <button className="fc-btn fc-btn-primary"
                    onClick={ this.toggleEdit.bind(this) } >Done</button>
          </div>
        </footer>
      );
    } else {
      body = <AddressDetails address={address} />;
      editButton = (
        <div>
          <button className="fc-btn icon-edit fc-right" onClick={this.toggleEdit.bind(this)}>
          </button>
        </div>
      );
    }

    return (
      <Panel className="fc-order-shipping-address"
             title="Shipping Address"
             controls={ editButton }
             enablePaddings={ true }>
        { body }
        { footer }
      </Panel>
    );
  }
}
