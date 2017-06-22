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
  onComplete: () => void,
  saveState: AsyncStatus,
  updateAddressState: AsyncStatus,
  t: any,
  actionHandler: () => void,
  actionTitle: string,
  applyAction: (id: number) => Promise<*>,
  inProfile: boolean,
  buttonLabel: string,
  deleteAddress: (id: number) => Promise<*>,
  restoreAddress: (id: number) => Promise<*>,
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
    const { activeAddressId } = this.state;
    const newActiveId = newAddress.isDefault ? id : activeAddressId;
    this.props.updateAddress(newAddress, id)
      .then(() => {
        this.setState({
          addressToEdit: {},
          isEditFormActive: false,
          newAddress: null,
          activeAddressId: newActiveId,
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
      this.props.applyAction(this.state.activeAddressId).then(this.props.onComplete);
    }
  }

  @autobind
  deleteAddress(address) {
    this.props.deleteAddress(address.id);
  }

  @autobind
  getActionsContent(address) {
    const { t } = this.props;

    if (address.isDeleted) {
      return (
        <div styleName="actions-block">
          <div styleName="link" onClick={() => this.props.restoreAddress(address.id)}>
            {t('Restore')}
          </div>
        </div>
      );
    }

    return (
      <div styleName="actions-block">
        <div styleName="link" onClick={() => this.editAddress(address)}>
          {t('Edit')}
        </div>
        <span styleName="separator">|</span>
        <div styleName="link" onClick={() => this.deleteAddress(address)}>
          {t('Remove')}
        </div>
      </div>
    );
  }

  @autobind
  getAddressInProfile(address: Object, checked: boolean, title: string) {
    const profileItemClasses = classNames(styles['profile-item'], {
      [styles.chosen]: checked,
    });
    const itemTitle = address.isDeleted ? <span styleName="deleted-content">{title}</span> : title;

    const deletedClass = classNames({
      [styles['deleted-content']]: address.isDeleted,
    });
    const content = (
      <AddressDetails
        address={address}
        hideName
        className={deletedClass}
      />
    );

    return (
      <li className={profileItemClasses} key={`address-radio-profile-${address.id}`}>
        <RadioButton
          id={`address-radio-profile-${address.id}`}
          name={`address-radio-profile-${address.id}`}
          checked={checked}
          disabled={address.isDeleted}
          onChange={() => this.changeAddressOption(address.id)}
        >
          <EditableBlock
            styleName="item-content"
            title={itemTitle}
            content={content}
            editAllowed={false}
          />
        </RadioButton>
        {this.getActionsContent(address)}
      </li>
    );
  }

  @autobind
  getAddress(address: Object, checked: boolean, title: string) {
    const itemClasses = classNames(styles.item, {
      [styles.chosen]: checked,
    });

    const content = (
      <AddressDetails
        address={address}
        hideName
      />
    );

    return (
      <li className={itemClasses} key={`address-radio-${address.id}`}>
        <RadioButton
          id={`address-radio-${address.id}`}
          name={`address-radio-${address.id}`}
          checked={checked}
          onChange={() => this.changeAddressOption(address.id)}
        >
          <EditableBlock
            isEditing={!_.isEmpty(this.state.addressToEdit)}
            styleName="item-content"
            title={title}
            content={content}
            editAction={() => this.editAddress(address)}
          />
        </RadioButton>
      </li>
    );
  }

  renderAddresses() {
    const { inProfile } = this.props;

    const items = _.map(this.props.addresses, (address) => {
      const title = address.isDefault ? `${address.name} (Default)` : address.name;
      const checked = address.id === this.state.activeAddressId;

      if (inProfile) {
        return this.getAddressInProfile(address, checked, title);
      }

      return this.getAddress(address, checked, title);
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
    const { addresses, actionHandler } = this.props;
    const isAdd = _.isEmpty(this.state.addressToEdit);
    const isRequired = _.isEmpty(addresses);
    const id = _.get(address, 'id');
    const action = {
      handler: isRequired ? actionHandler : this.cancelEditing,
      title: isRequired ? 'Close' : 'Cancel',
    };
    const title = isAdd ? 'Add Address' : 'Edit Address';


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
      handler: props.actionHandler,
      title: props.actionTitle,
    };

    return (
      <CheckoutForm
        submit={this.saveAndContinue}
        title="Shipping Addresses"
        error={_.get(props.saveState, 'err', null)}
        inProgress={_.get(props.saveState, 'inProgress', false)}
        buttonLabel={props.buttonLabel}
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
