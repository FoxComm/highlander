import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import AddressForm from './address-form';
import AddressDetails from './address-details';
import ConfirmationDialog from '../modal/confirmation-dialog';
import { autobind } from 'core-decorators';

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
    onSelectAddress: PropTypes.func,
    onDeleteAddress: PropTypes.func,
    isSelected: PropTypes.bool
  };

  constructor(props, context) {
    super(props, context);
    this.onSelectAddress = props.onSelectAddress && props.onSelectAddress.bind(this, props.address);
    this.onDeleteAddress = props.onDeleteAddress &&
      props.onDeleteAddress.bind(this, props.address);
  }

  toggleEdit() {
    dispatch('toggleModal', <AddressForm address={this.props.address} customerId={this.props.customerId}
                              onSaved={this.onSelectAddress} />);
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
      'is-active': this.props.isSelected
    });

    return (
      <li className={classes}>
        <div className="fc-address-controls">
          <button className="fc-btn icon-trash" onClick={this.deleteAddress.bind(this)}></button>
          <button className="fc-btn icon-edit" onClick={this.toggleEdit.bind(this)}></button>
        </div>
        <div>
          { isDefault }
          <AddressDetails address={address} />
        </div>
        { choose }
      </li>
    );
  }
}
