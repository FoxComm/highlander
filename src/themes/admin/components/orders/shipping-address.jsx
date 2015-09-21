'use strict';

import React from 'react';
import Addresses from '../addresses/addresses';

export default class OrderShippingAddress extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      isEditing: false
    };
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
      body = <Addresses order={this.props.order} />;
      actions = (
        <footer>
          <button className="fc-btn fc-btn-primary" onClick={this.toggleEdit.bind(this)}>Done</button>
        </footer>
      );
    } else {
      body = (
        <div>
          <div className="address-line">{address.name}</div>
          <div className="address-line">{address.address1}</div>
          <div className="address-line">{address.address2}</div>
          <div className="address-line">{address.city}, {address.state} {address.zip}</div>
          <div className="address-line">{address.country}</div>
        </div>
      );
      editButton = (
        <button className="fc-btn fc-btn-primary fc-edit-button" onClick={this.toggleEdit.bind(this)}>
          <i className="fa fa-pencil"></i>
        </button>
      );
    }

    return (
      <section className="fc-content-box fc-order-shipping-address">
        <header className="header">
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
