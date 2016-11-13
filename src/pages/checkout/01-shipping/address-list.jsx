
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';

// components
import EditableBlock from 'ui/editable-block';
import EditAddress from 'ui/address/edit-address';
import CheckoutForm from '../checkout-form';
import RadioButton from 'ui/radiobutton/radiobutton';
import { AddressDetails } from 'ui/address';

// styles
import styles from './address-list.css';

type Props = {
  activeAddressId?: number|string,
  addresses: Array<any>,
  collapsed: boolean,
  continueAction: Function,
  editAction: Function,
  updateAddress: Function,
  t: any,
};

type State = {
  addressToEdit: Object,
  newAddress: Object,
  isEditFormActive: boolean,
  activeAddressId?: number|string,
};

class AddressList extends Component {
  props: Props;

  state: State = {
    addressToEdit: {},
    newAddress: {},
    activeAddressId: this.props.activeAddressId,
    isEditFormActive: false,
  };

  componentWillMount() {
    if (_.isEmpty(this.props.addresses)) {
      this.addAddress();
    }

    if (this.props.addresses.length >= 1) {
      const defaultAddress = _.find(this.props.addresses, { isDefault: true });
      const selected = defaultAddress ? defaultAddress.id : this.props.addresses[0].id;
      this.changeAddressOption(selected);
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
      const selected = defaultAddress ? defaultAddress.id : nextProps.addresses[0].id;
      this.changeAddressOption(selected);
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
    this.props.updateAddress(this.state.newAddress, id)
      .then(() => {
        this.setState({
          addressToEdit: {},
          isEditFormActive: false,
        });
      })
      .catch((error) => {
        this.setState({
          error,
        });
      }
    );
  }

  @autobind
  changeAddressOption(id) {
    this.setState({
      activeAddressId: id,
    });
  }

  @autobind
  saveAndContinue() {
    this.props.continueAction(this.state.activeAddressId);
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
        action={action}
        error={this.state.error}
      >
        <EditAddress address={address} onUpdate={this.setNewAddress} />
      </CheckoutForm>
    );
  }

  renderList() {
    return (
      <CheckoutForm
        submit={this.saveAndContinue}
        title="SHIPPING ADDRESS"
        error={this.props.error}
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
