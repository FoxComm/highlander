'use strict';

import React from 'react';
import Addresses from '../addresses/addresses';
import AddressDetails from '../addresses/address-details';
import OrderStore from '../../stores/orders';

export default class OrderShippingAddress extends React.Component {

  constructor(props) {
    super(props);
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
      <section className="fc-content-box fc-order-shipping-address">
        <header>
          <div className='fc-grid'>
            <div className="fc-col-2-3">Shipping Address</div>
            <div className="fc-col-1-3 fc-controls">
              {editButton}
            </div>
          </div>
        </header>
        <article>
          {body}
          {actions}
        </article>
      </section>
    );
  }
}

OrderShippingAddress.propTypes = {
  order: React.PropTypes.object,
  isEditing: React.PropTypes.bool
};
