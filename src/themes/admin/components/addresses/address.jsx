'use strict';

import React from 'react';
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
        <div><input type="checkbox" defaultChecked={address.isDefault} disabled />Default Address</div>
    );
    let address2 = (val) => {
      return <span><span>{val}</span><br/></span>;
    };
    let choose = null;
    if (this.props.order) {
      let onClick = this.setActiveAddress.bind(this);
      choose = (
        <button className="fc-btn fc-address-choose" onClick={onClick} disabled={address.isActive}>
          Choose
        </button>
      );
    }

    let classes = ['fc-address'];

    if (address.isActive) classes.push('is-active');

    return (
      <li className={`${classes.join(' ')}`}>
        <div className="fc-address-controls">
          <button onClick={this.toggleEdit.bind(this)}><i className="fa fa-pencil"></i></button>
        </div>
        <div >
          <div className="fc-address-details">
            { address.isDefault ? isDefault : '' }
            <p><strong>{address.name}</strong></p>
            <p>
              <span>{address.address1}</span><br />
              { address.address2 ? address2(address.address2) : '' }
              <span>{address.city}</span>, <span>{address.state}</span> <span>{address.zip}</span><br />
              <span>{address.country}</span>
            </p>
          </div>
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
