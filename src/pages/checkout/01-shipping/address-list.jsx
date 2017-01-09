// @flow weak

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';
import { lookupAddressId } from 'paragons/address';

// components
import EditableBlock from 'ui/editable-block';
import EditAddress from 'ui/address/edit-address';
import CheckoutForm from '../checkout-form';
import RadioButton from 'ui/radiobutton/radiobutton';
import { AddressDetails } from 'ui/address';

// styles
import styles from './address-list.css';

import type { Address } from 'types/address';
import type { AsyncStatus } from 'types/async-actions';

type Props = {
  activeAddress?: Address,
  addresses: Array<any>,
  collapsed: boolean,
  saveShippingAddress: (id: number) => Promise,
  updateAddress: (address: Address, id?: number) => Promise,
  editAction: Function,
  onComplete: () => void,
  saveShippingState: AsyncStatus,
  t: any,
};

type State = {
  addressToEdit: Object,
  newAddress: Object|null,
  isEditFormActive: boolean,
  activeAddressId: number|null,
  error?: any,
};

class AddressList extends Component {
  props: Props;

  state: State = {
    addressToEdit: {},
    newAddress: null,
    activeAddressId: this.lookupAddressId(this.props.activeAddress),
    isEditFormActive: false,
  };

  // IDs in cart.shippingAddress and in addresses DON'T match!
  lookupAddressId(address: ?Address): null|number {
    return lookupAddressId(this.props.addresses, address);
  }

  componentWillMount() {
    if (_.isEmpty(this.props.addresses)) {
      this.addAddress();
    }

    this.autoSelectAddress(this.props);
  }

  autoSelectAddress(props: Props) {
    if (props.activeAddress) {
      const addressId = this.lookupAddressId(props.activeAddress);
      if (addressId != null) {
        return this.changeAddressOption(addressId);
      }
    }
    if (props.addresses.length >= 1) {
      const defaultAddress = _.find(props.addresses, { isDefault: true });
      const activeAddressId = defaultAddress ? defaultAddress.id : props.addresses[0].id;
      this.changeAddressOption(activeAddressId);
    }
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.activeAddress != this.props.activeAddress) {
      this.autoSelectAddress(nextProps);
    }
  }

  componentWillUpdate(nextProps: Props, nextState: State) {
    if (this.state.isEditFormActive == nextState.isEditFormActive &&
        this.props.addresses !== nextProps.addresses &&
        nextProps.addresses.length > 0) {
      this.setState({
        addressToEdit: {},
        isEditFormActive: false,
      });
    }

    const selectedAddress = _.find(nextProps.addresses, { id: nextState.activeAddressId });
    if (!nextState.isEditFormActive && nextProps.addresses.length > 0 && !selectedAddress) {
      const defaultAddress = _.find(nextProps.addresses, { isDefault: true });
      const selectedAddressId = defaultAddress ? defaultAddress.id : nextProps.addresses[0].id;
      this.changeAddressOption(selectedAddressId);
    }
  }

  @autobind
  editAddress(address) {
    this.setState({
      addressToEdit: address,
      isEditFormActive: true,
    });
  }

  @autobind
  addAddress() {
    this.setState({
      isEditFormActive: true,
    });
  }

  @autobind
  finishEditingAddress(id) {
    const newAddress = this.state.newAddress || this.state.addressToEdit;
    this.props.updateAddress(newAddress, id)
      .then(() => {
        this.setState({
          addressToEdit: {},
          isEditFormActive: false,
        });
      })
      .catch((err) => {
        let error = err;
        const messages = _.get(err, ['responseJson', 'errors'], [err.toString()]);
        const zipErrorPresent = _.find(messages,
          (msg) => { return msg.indexOf('zip') >= 0; });

        if (zipErrorPresent) {
          error = new Error(messages);
          // $FlowFixMe: no such field
          error.responseJson = { errors: ['Zip code is invalid']};
        }
        this.setState({
          error,
        });
      }
    );
  }

  @autobind
  changeAddressOption(id: number) {
    this.setState({
      activeAddressId: id,
    });
  }

  @autobind
  saveAndContinue() {
    if (this.state.activeAddressId != null) {
      this.props.saveShippingAddress(this.state.activeAddressId).then(this.props.onComplete);
    }
  }

  renderAddresses() {
    const items = _.map(this.props.addresses, (address, key) => {
      const content = <AddressDetails address={address} hideName />;
      const checked = address.id === this.state.activeAddressId;

      return (
        <li styleName="item" key={`address-radio-${key}`}>
          <RadioButton
            id={`address-radio-${key}`}
            name={`address-radio-${key}`}
            checked={checked}
            onChange={() => this.changeAddressOption(address.id)}
          >
            <EditableBlock
              isEditing={!_.isEmpty(this.state.addressToEdit)}
              styleName="item-content"
              title={address.name}
              content={content}
              editAction={() => this.editAddress(address)}
            />
          </RadioButton>
        </li>
      );
    });

    return (
      <div>
        <ul styleName="list">{items}</ul>
        <button styleName="add-address-button" type="button" onClick={this.addAddress}>
          Add Address
        </button>
      </div>
    );
  }

  @autobind
  cancelEditing() {
    this.setState({
      addressToEdit: {},
      isEditFormActive: false,
    });
  }

  @autobind
  setNewAddress(address) {
    this.setState({
      newAddress: address,
    });
  }

  renderEditingForm(address) {
    const id = _.get(address, 'id');
    const title = _.isEmpty(this.state.addressToEdit) ? 'Add Address' : 'Edit Address';
    const action = {
      action: this.cancelEditing,
      title: 'Cancel',
    };

    return (
      <CheckoutForm
        submit={() => this.finishEditingAddress(id)}
        title={title}
        buttonLabel="Save Address"
        action={action}
        error={this.state.error}
      >
        <EditAddress address={address} onUpdate={this.setNewAddress} />
      </CheckoutForm>
    );
  }

  renderList() {
    const { props } = this;
    return (
      <CheckoutForm
        submit={this.saveAndContinue}
        title="SHIPPING ADDRESS"
        error={_.get(props.saveShippingState, 'err')}
        inProgress={_.get(props.saveShippingState, 'inProgress', false)}
      >
        {this.renderAddresses()}
      </CheckoutForm>
    );
  }

  render() {
    return this.state.isEditFormActive ? this.renderEditingForm(this.state.addressToEdit) : this.renderList();
  }
}

export default localized(AddressList);
