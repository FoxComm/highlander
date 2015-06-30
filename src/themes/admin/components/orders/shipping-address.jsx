'use strict';

import React from 'react';
import Addresses from '../addresses/addresses';

export default class OrderShippingAddress extends React.Component {
  render() {
    let
      address = this.props.order.shippingAddress,
      innercontent = null;

    if (this.props.isEditing) {
      innercontent = <Addresses order={this.props.order} />;
    } else {
      innercontent = (
        <div>
          <header>Shipping Address</header>
          <div className="address-line">{address.name}</div>
          <div className="address-line">{address.street1}</div>
          <div className="address-line">{address.street2}</div>
          <div className="address-line">{address.city}, {address.state} {address.zip}</div>
          <div className="address-line">{address.country}</div>
        </div>
      );
    }

    return (
      <section id="order-shipping-address">
        {innercontent}
      </section>
    );
  }
}

OrderShippingAddress.propTypes = {
  order: React.PropTypes.object,
  isEditing: React.PropTypes.bool
};
