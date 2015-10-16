'use strict';

import React, { PropTypes } from 'react';
import Addresses from '../addresses/addresses';
import AddressDetails from '../addresses/address-details';
import OrdersActions from '../../actions/orders';
import AddressStore from '../../stores/addresses';
import ContentBox from '../content-box/content-box';

export default class OrderShippingAddress extends React.Component {

  static propTypes = {
    order: PropTypes.object.isRequired,
    isEditing: PropTypes.bool
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      isEditing: false
    };
  }

  onSelectAddress(address) {
    OrdersActions.setShippingAddress(this.props.order.referenceNumber, address.id);
  }

  onDeleteAddress(address) {
    if (address.id === this.props.order.shippingAddress.id) {
      OrdersActions.removeShippingAddress(this.props.order.referenceNumber);
    }
    AddressStore.delete(this.props.order.customer.id, address.id);
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  isAddressSelected(address) {
    return this.props.order ? address.id === this.props.order.shippingAddress.id : false;
  }

  render() {
    let address = this.props.order.shippingAddress;
    let body = null;
    let actions = null;
    let editButton = null;

    if (this.state.isEditing) {
      body = (
        <Addresses order={this.props.order}
                   isAddressSelected={this.isAddressSelected.bind(this)}
                   onSelectAddress={this.onSelectAddress.bind(this)}
                   onDeleteAddress={this.onDeleteAddress.bind(this)} />
      );
      actions = (
        <footer>
          <button className="fc-btn fc-btn-primary" onClick={this.toggleEdit.bind(this)}>Done</button>
        </footer>
      );
    } else {
      body = (
        <AddressDetails address={address} />
      );
      editButton = (
        <button className="fc-btn fc-edit-button icon-edit" onClick={this.toggleEdit.bind(this)}>
        </button>
      );
    }

    return (
      <ContentBox
        title="Shipping Address"
        actionBlock={editButton}
        className="fc-order-shipping-address">
        {body}
        {actions}
      </ContentBox>
    );
  }
}
