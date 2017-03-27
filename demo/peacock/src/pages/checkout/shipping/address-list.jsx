/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';
import { lookupAddressId } from 'paragons/address';
import classNames from 'classnames';

// components
import EditableBlock from 'ui/editable-block';
import EditAddress from 'ui/address/edit-address';
import CheckoutForm from 'pages/checkout/checkout-form';
import RadioButton from 'ui/radiobutton/radiobutton';
import { AddressDetails } from 'ui/address';
import ActionLink from 'ui/action-link/action-link';

// types
import type { Address } from 'types/address';
import type { AsyncStatus } from 'types/async-actions';

import styles from './address-list.css';

type Props = {
  activeAddress?: Address | {},
  addresses: Array<any>,
  collapsed: boolean,
  saveShippingAddress: (id: number) => Promise<*>,
  updateAddress: (address: Address, id?: number) => Promise<*>,
  editAction: Function,
  onComplete: () => void,
  toggleShippingModal: Function,
  saveShippingState: AsyncStatus,
  updateAddressState: AsyncStatus,
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
    if (!_.isEmpty(props.activeAddress)) {
      const addressId = this.lookupAddressId(props.activeAddress);

      if (addressId != null) {
        return this.changeAddressOption(addressId);
      }
    } else if (props.addresses.length > 0) {
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
      const itemClasses = classNames(styles.item, {
        [styles.chosen]: checked,
      });

      return (
        <li className={itemClasses} key={`address-radio-${key}`}>
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

    const icon = {
      name: 'fc-plus',
      className: styles['plus-icon'],
    };

    return (
      <div>
        <ul styleName="list">
          {items}
        </ul>
        <ActionLink
          action={this.addAddress}
          title="Add new"
          icon={icon}
          styleName="action-link-add"
        />
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
    const action = {
      handler: this.cancelEditing,
      title: 'Cancel',
    };
    const title = _.isEmpty(this.state.addressToEdit) ? 'Add Address' : 'Edit Address';


    return (
      <CheckoutForm
        submit={() => this.finishEditingAddress(id)}
        buttonLabel="Save address"
        title={title}
        action={action}
        error={this.state.error}
        inProgress={_.get(this.props.updateAddressState, 'inProgress', false)}
      >
        <div styleName="edit-form">
          <EditAddress address={address} onUpdate={this.setNewAddress} />
        </div>
      </CheckoutForm>
    );
  }

  renderList() {
    const { props } = this;
    const action = {
      handler: props.toggleShippingModal,
      title: 'Close',
    };

    return (
      <CheckoutForm
        submit={this.saveAndContinue}
        title="Shipping address"
        error={_.get(props.saveShippingState, 'err')}
        inProgress={_.get(props.saveShippingState, 'inProgress', false)}
        buttonLabel="Apply"
        action={action}
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
