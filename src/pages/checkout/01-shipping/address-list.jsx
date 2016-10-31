
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';

// components
import EditableBlock from 'ui/editable-block';
import EditAddress from '../address/edit-address';
import CheckoutForm from '../checkout-form';
import ViewAddress from '../address/view-address';
import RadioButton from 'ui/radiobutton/radiobutton';

import { AddressKind } from 'modules/checkout';

// styles
import styles from './address-list.css';

type Props = {
  activeAddress?: number|string,
  addresses: Array<any>,
  collapsed: boolean,
  continueAction: Function,
  editAction: Function,
  updateAddress: Function,
  inProgress: boolean,
  t: any,
};

type State = {
  addressToEdit: Object,
  isEditFormActive: boolean,
  activeAddress?: number|string,
};

class AddressList extends Component {
  props: Props;

  state: State = {
    addressToEdit: {},
    activeAddress: this.props.activeAddress,
    isEditFormActive: false,
  };

  componentWillMount() {
    if (_.isEmpty(this.props.addresses)) {
      this.addAddress();
    }

    if (this.props.addresses.length == 1) {
      this.changeAddressOption(this.props.addresses[0].id);
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
    this.props.updateAddress(id)
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
      activeAddress: id,
    });
  }

  @autobind
  saveAndContinue() {
    this.props.continueAction(this.state.activeAddress);
  }

  renderAddresses() {
    const items = _.map(this.props.addresses, (address, key) => {
      const content = <ViewAddress { ...address } hideName />;
      const checked = address.id === this.state.activeAddress;

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
        <EditAddress {...this.props} address={address} addressKind={AddressKind.SHIPPING} />
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
