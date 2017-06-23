/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';
import { connect } from 'react-redux';
import { lookupAddressId } from 'paragons/address';

// actions
import * as checkoutActions from 'modules/checkout';
import * as actions from 'modules/profile';

// components
import { AddressDetails } from 'ui/address';
import DetailsBlock from '../details-block';
import AddressList from 'ui/address/address-list';
import Loader from 'ui/loader';

// types
import type { Address } from 'types/address';
import type { AsyncStatus } from 'types/async-actions';

import styles from '../profile.css';

type ActionDetails = {
  title: string,
  icon?: {
    name: string,
    className: string,
  },
};

type Props = {
  fetchAddresses: () => Promise<*>,
  addresses: Array<Address>,
  shippingAddress: Address,
  deleteAddress: (id: number) => Promise<*>,
  restoreAddress: (id: number) => Promise<*>,
  setAddressAsDefault: (id: number) => Promise<*>,
  cleanDeletedAddresses: () => void,
  t: any,
  className: string,
  setAsDefaultState: AsyncStatus,
  updateAddressState: AsyncStatus,
  cartState: boolean,
  toggleAddressesModal: () => void,
  addressesModalVisible: boolean,
};

class AddressBlock extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchAddresses();
  }

  componentWillUnmount() {
    this.props.cleanDeletedAddresses();
  }

  get defaultAddress() {
    const { addresses } = this.props;
    return _.find(addresses, { isDefault: true });
  }

  @autobind
  displayShippingAddress() {
    const defaultAddress = this.defaultAddress;

    if (_.isEmpty(defaultAddress)) return true;

    const { addresses, shippingAddress } = this.props;
    const defaultSameAsShipping = defaultAddress.id === lookupAddressId(addresses, shippingAddress);

    return !defaultSameAsShipping;
  }

  get shippingAddressDetails() {
    const { shippingAddress } = this.props;
    const defaultAddress = this.defaultAddress;

    if (!this.displayShippingAddress() || _.isEmpty(shippingAddress) && _.isEmpty(defaultAddress)) {
      return null;
    } else if (_.isEmpty(shippingAddress)) {
      return (
        <div>No shipping address found.</div>
      );
    }

    return (
      <AddressDetails
        address={shippingAddress}
      />
    );
  }

  get defaultAddressDetails() {
    const { shippingAddress } = this.props;
    const defaultAddress = this.defaultAddress;

    if (_.isEmpty(shippingAddress) && _.isEmpty(defaultAddress)) {
      return (
        <div> No address yet.</div>
      );
    } else if (_.isEmpty(defaultAddress)) {
      return (
        <div>No default address found.</div>
      );
    }

    return (
      <AddressDetails
        address={defaultAddress}
      />
    );
  }

  get addressDetails() {
    const { cartState } = this.props;

    if (!cartState) return <Loader size="m" />;

    const defaultAddress = this.defaultAddressDetails;
    const shippingAddress = this.shippingAddressDetails;

    return (
      <div>
        <div styleName="divider top" />
        {defaultAddress}
        {shippingAddress ? <div styleName="divider addresses" /> : null}
        {shippingAddress}
      </div>
    );
  }

  get addressesModalContent() {
    const { toggleAddressesModal, setAsDefaultState } = this.props;

    return (
      <AddressList
        actionHandler={toggleAddressesModal}
        actionTitle="Close"
        applyAction={this.props.setAddressAsDefault}
        onComplete={toggleAddressesModal}
        saveState={setAsDefaultState}
        buttonLabel="Set as default"
        inProfile
        {...this.props}
      />
    );
  }

  get actionDetails() {
    const { addresses } = this.props;

    if (addresses.length > 0) {
      return {
        title: 'Edit',
      };
    }

    return {
      title: 'Add new',
      icon: {
        name: 'fc-plus',
        className: styles.plus,
      },
    };
  }

  render() {
    const { className, toggleAddressesModal, addressesModalVisible, cartState } = this.props;
    const actionDetails: ActionDetails = this.actionDetails;

    return (
      <div className={className}>
        <DetailsBlock
          data={this.addressDetails}
          toggleModal={toggleAddressesModal}
          modalVisible={addressesModalVisible}
          actionTitle={actionDetails.title}
          actionIcon={actionDetails.icon}
          modalContent={this.addressesModalContent}
          blockTitle="Shipping addresses"
          hideAction={!cartState}
        />
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    addresses: _.get(state.checkout, 'addresses', []),
    shippingAddress: _.get(state.cart, 'shippingAddress', {}),
    addressesModalVisible: _.get(state.profile, 'addressesModalVisible', false),
    setAsDefaultState: _.get(state.asyncActions, 'setAddressAsDefault', {}),
    updateAddressState: _.get(state.asyncActions, 'updateAddress', {}),
    cartState: _.get(state.asyncActions, 'cart.finished', false),
  };
};

export default connect(mapStateToProps, {
  ...checkoutActions,
  ...actions,
})(localized(AddressBlock));
