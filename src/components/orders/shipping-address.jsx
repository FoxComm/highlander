'use strict';

import React from 'react';
import Addresses from '../addresses/addresses';
import AddressDetails from '../addresses/address-details';
import ContentBox from '../panel/panel';
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
    let actions = null;
    let editButton = null;

    if (this.state.isEditing) {
      body = <Addresses order={this.props.order} onSelectAddress={this.onSelectAddress.bind(this)} />;
      editButton = (
        <button className="fc-btn fc-btn-plain icon-chevron-up" onClick={this.toggleEdit.bind(this)}></button>
      );
    } else {
      body = (
        <AddressDetails address={address} />
      );
      editButton = (
        <button className="fc-btn fc-btn-plain icon-chevron-down" onClick={this.toggleEdit.bind(this)}>
        </button>
      );
    }

    return (
      <ContentBox className="fc-order-shipping-address"
                  title="Shipping Address"
                  actionBlock={ editButton }
                  enablePaddings={ true }>
        {body}
      </ContentBox>
    );
  }
}

OrderShippingAddress.propTypes = {
  order: React.PropTypes.object,
  isEditing: React.PropTypes.bool
};
