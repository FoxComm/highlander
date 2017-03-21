/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import localized from 'lib/i18n';
import { connect } from 'react-redux';

// components
// import EditableBlock from 'ui/editable-block';
import { AddressDetails } from 'ui/address';
// import AddressList from './address-list';
import GuestShipping from './guest-shipping';
import Icon from 'ui/icon';

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
  isGuestMode: boolean,
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

  get action() {
    const { props } = this;

    if (!_.isEmpty(props.shippingAddress)) {
      return (
        <div styleName="btn-action">Change</div>
      );
    } else if (_.isEmpty(props.shippingAddress) && props.addresses.length > 0) {
      return (
        <div styleName="btn-action">Choose</div>
      );
    }
    return (
      <div styleName="btn-action"><Icon styleName="plus" name="fc-plus"/> Add new</div>
    );
  }

  get content() {
    const { props } = this;
    const savedAddress = props.shippingAddress;

    if (!_.isEmpty(savedAddress)) {
      return (
        <AddressDetails address={savedAddress} styleName="savedAddress" />
      );
    }

    if (props.isGuestMode) {
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
  }

  render() {
    // const { t } = this.props;

    return (
      <div>
        <div styleName="header">
          <span styleName="title">Shipping address</span>
          {this.action}
        </div>
        {this.content}
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
