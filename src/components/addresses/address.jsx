'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import AddressStore from '../../stores/addresses';
import { dispatch } from '../../lib/dispatcher';
import AddressForm from './address-form.jsx';
import AddressDetails from './address-details.jsx';
import ConfirmModal from '../modal/confirm';

const confirmDeleteOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to delete this address?',
  cancel: 'Cancel',
  proceed: 'Yes, Delete'
};

export default class Address extends React.Component {

  static propTypes = {
    address: PropTypes.object,
    order: PropTypes.object,
    customerId: PropTypes.number.isRequired,
    onSelectAddress: PropTypes.func,
    onDeleteAddress: PropTypes.func
  };

  constructor(props, context) {
    super(props, context);
    this.onSelectAddress = props.onSelectAddress && props.onSelectAddress.bind(this, props.address);
    this.onDeleteAddress = props.onDeleteAddress && props.onDeleteAddress.bind(this, props.address, this.isSelectedForOrder());
  }

  toggleEdit() {
    dispatch('toggleModal', <AddressForm address={this.props.address} customerId={this.props.customerId}
                              onSaved={this.onSelectAddress} />);
  }

  isSelectedForOrder() {
    return this.props.order ? this.props.address.id === this.props.order.shippingAddress.id : false;
  }

  deleteAddress() {
    dispatch('toggleModal', <ConfirmModal details={confirmDeleteOptions} callback={this.onDeleteAddress} />);
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
      'is-active': this.isSelectedForOrder()
    });

    return (
      <li className={classes}>
        <div className="fc-address-controls">
          <button className="fc-btn icon-trash" onClick={this.deleteAddress.bind(this)}></button>
          <button className="fc-btn icon-edit" onClick={this.toggleEdit.bind(this)}></button>
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
