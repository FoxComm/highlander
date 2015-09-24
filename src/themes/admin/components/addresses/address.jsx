'use strict';

import _ from 'lodash';
import React from 'react';
import classNames from 'classnames';
import AddressStore from '../../stores/addresses';
import { dispatch } from '../../lib/dispatcher';
import AddressForm from './address-form.jsx';
import AddressDetails from './address-details.jsx';

class Address extends React.Component {

  constructor(props) {
    super(props);
    this.onSelectAddress = props.onSelectAddress && props.onSelectAddress.bind(this, props.address);
  }

  toggleEdit() {
    dispatch('toggleModal', <AddressForm address={this.props.address} customerId={this.props.customerId}
                              onSaved={this.onSelectAddress} />);
  }

  render() {
    let address = this.props.address;

    let isDefault = (
        <label className="fc-address-default">
          <input type="checkbox" defaultChecked={address.isDefault} disabled />
          <span>Default shipping address</span>
        </label>
    );
    let choose = null;
    if (this.props.order) {
      choose = (
        <button className="fc-btn fc-address-choose" onClick={this.onSelectAddress} disabled={address.isActive}>
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
          <button className="icon-edit" onClick={this.toggleEdit.bind(this)}></button>
        </div>
        <div>
          { address.isDefault ? isDefault : '' }
          <AddressDetails address={address} />
        </div>
        { choose }
      </li>
    );
  }
}

Address.propTypes = {
  address: React.PropTypes.object,
  order: React.PropTypes.object,
  customerId: React.PropTypes.number.isRequired,
  onSelectAddress: React.PropTypes.func
};

export default Address;
