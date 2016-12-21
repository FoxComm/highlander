/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import localized from 'lib/i18n';
import { connect } from 'react-redux';
import { isGuest } from 'paragons/auth';

// components
import EditableBlock from 'ui/editable-block';
import { AddressDetails } from 'ui/address';
import AddressList from './address-list';
import GuestShipping from './guest-shipping';

import { saveShippingAddress, updateAddress, addShippingAddress, updateShippingAddress } from 'modules/checkout';
import type { Address } from 'types/address';
import type { AsyncStatus } from 'types/async-actions';

// styles
import styles from './shipping.css';

type Props = {
  addresses: Array<any>,
  collapsed: boolean,
  onComplete: Function,
  editAction: Function,
  fetchAddresses: Function,
  isEditing: boolean,
  t: any,
  shippingAddress: Object,
  addShippingAddress: (address: Address) => Promise,
  updateShippingAddress: (address: Address) => Promise,
  saveShippingAddress: (id: number) => Promise,
  saveShippingState: AsyncStatus,
  updateAddress: (address: Address, id?: number) => Promise,
  auth: ?Object,
};

function mapStateToProps(state) {
  return {
    saveShippingState: _.get(state.asyncActions, 'saveShippingAddress', {}),
  };
}

class Shipping extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchAddresses();
  }

  componentWillUpdate(nextProps : Props) {
    if (nextProps.auth !== this.props.auth) {
      this.props.fetchAddresses();
    }
  }

  get isGuestMode(): boolean {
    return isGuest(this.props.auth);
  }

  content() {
    const { props } = this;
    const savedAddress = props.shippingAddress;

    if ((!_.isEmpty(savedAddress) && !props.isEditing)) {
      return (
        <AddressDetails address={savedAddress} styleName="savedAddress" />
      );
    }

    if (this.isGuestMode) {
      return (
        <GuestShipping
          addShippingAddress={props.addShippingAddress}
          updateShippingAddress={props.updateShippingAddress}
          shippingAddress={props.shippingAddress}
          auth={props.auth}
          onComplete={props.onComplete}
        />
      );
    }

    return (
      <AddressList { ...props } activeAddress={savedAddress}/>
    );
  }

  render() {
    const { t } = this.props;

    return (
      <div >
        <EditableBlock
          isEditing={this.props.isEditing}
          styleName="shipping"
          title={t('SHIPPING')}
          content={this.content()}
          editAction={this.props.editAction}
        />
      </div>
    );
  }
}

export default _.flowRight(
  localized,
  connect(mapStateToProps, {
    updateShippingAddress,
    addShippingAddress,
    saveShippingAddress,
    updateAddress,
  })
)(Shipping);
