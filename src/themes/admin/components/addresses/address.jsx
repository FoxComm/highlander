'use strict';

import React from 'react';
import classNames from 'classnames';
import AddressStore from '../../stores/addresses';
import OrderStore from '../../stores/orders'
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';
import AddressForm from './address-form.jsx';
import _ from 'lodash';

class Address extends React.Component {

  setActiveAddress() {
    OrderStore.setShippingAddress(this.props.order.referenceNumber, this.props.address.id);
  }

  toggleEdit() {
    dispatch('toggleModal', <AddressForm address={this.props.address} customerId={this.props.customerId}/>);
  }

  render() {
    let address = this.props.address;

    let isDefault = (
        <label className="fc-address-default">
          <input type="checkbox" defaultChecked={address.isDefault} disabled />
          <span>Default Address</span>
        </label>
    );
    let choose = null;
    if (this.props.order) {
      let onClick = this.setActiveAddress.bind(this);
      choose = (
        <button className="fc-btn fc-address-choose" onClick={onClick} disabled={address.isActive}>
          Choose
        </button>
      );
    }

    let classes = classNames({
      'fc-address': true,
      'is-active': this.props.order ? address.id === this.props.order.shippingAddress.id : false
    });

    return (
      <li className={classes}>
        <div className="fc-address-controls">
          <button onClick={this.toggleEdit.bind(this)}><i className="fa fa-pencil"></i></button>
        </div>
        <div >
          <ul className="fc-address-details">
            { address.isDefault ? isDefault : '' }
            <li className="name"><strong>{address.name}</strong></li>
            <li>{address.address1}</li>
            { address.address2 ? <li>{address.address2}</li> : '' }
            <li>
              {address.city}, <span>{address.region.name}</span> <span>{address.zip}</span>
            </li>
            <li>{address.country}</li>
            { address.phoneNumber ? <li>{address.phoneNumber}</li> : '' }
          </ul>
        </div>
        { choose }
      </li>
    );
  }
}

Address.propTypes = {
  address: React.PropTypes.object,
  order: React.PropTypes.object,
  customerId: React.PropTypes.number.isRequired
};

export default Address;
