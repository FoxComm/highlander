'use strict';

import React from 'react';

export default class OrderShippingAddress extends React.Component {
  render() {
    let address = this.props.order.shippingAddress;

    return (
      <section id="order-shipping-address">
        <header>Shipping Address</header>
        <div className="address-line">{address.name}</div>
        <div className="address-line">{address.street1}</div>
        <div className="address-line">{address.street2}</div>
        <div className="address-line">{address.city}, {address.state} {address.zip}</div>
        <div className="address-line">{address.country}</div>
      </section>
    );
  }
}

OrderShippingAddress.propTypes = {
  order: React.PropTypes.object
};
