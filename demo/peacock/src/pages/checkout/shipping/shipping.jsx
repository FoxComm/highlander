/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import localized from 'lib/i18n';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import { AddressDetails } from 'ui/address';
import AddressList from 'ui/address/address-list';
import Modal from 'ui/modal/modal';
import ActionLink from 'ui/action-link/action-link';
import Loader from 'ui/loader';

// actions and types
import * as checkoutActions from 'modules/checkout';
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
  addShippingAddress: (address: Address) => Promise<*>,
  updateShippingAddress: (address: Address) => Promise<*>,
  saveShippingAddress: (id: number) => Promise<*>,
  toggleShippingModal: Function,
  shippingModalVisible: boolean,
  saveShippingState: AsyncStatus,
  updateAddressState: AsyncStatus,
  cartChangeState: AsyncStatus,
  updateAddress: (address: Address, id?: number) => Promise<*>,
  auth: ?Object,
  isGuestMode: boolean,
};

type State = {
  fetchedAddresses: boolean,
};

class Shipping extends Component {
  props: Props;

  state: State = {
    fetchedAddresses: false,
  };

  componentWillMount() {
    this.fetchAddresses();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.auth !== this.props.auth) {
      this.setState({ fetchedAddresses: false });
    }
    if (nextProps.cartChangeState.finished && !this.state.fetchedAddresses) {
      this.fetchAddresses();
    }
  }

  @autobind
  fetchAddresses() {
    this.props.fetchAddresses().then(() => {
      this.setState({ fetchedAddresses: true });
    });
  }

  get action() {
    const { props } = this;
    let title;
    let icon;

    if (this.state.fetchedAddresses) {
      if (!_.isEmpty(props.shippingAddress) || props.addresses.length > 0) {
        title = 'Choose';
      } else {
        title = 'Add new';
        icon = {
          name: 'fc-plus',
          className: styles.plus,
        };
      }

      return (
        <ActionLink
          action={props.toggleShippingModal}
          title={title}
          styleName="action-link-addresses"
          icon={icon}
        />
      );
    }
  }

  get addressDetails() {
    if (!_.isEmpty(this.props.shippingAddress)) {
      return (
        <AddressDetails
          address={this.props.shippingAddress}
          styleName="shippingAddress"
        />
      );
    }
  }

  get content() {
    const { toggleShippingModal, shippingModalVisible, shippingAddress } = this.props;

    if (this.state.fetchedAddresses) {
      return (
        <div>
          {this.addressDetails}
          <Modal
            show={shippingModalVisible}
            toggle={toggleShippingModal}
          >
            <AddressList
              activeAddress={shippingAddress}
              actionTitle="Close"
              actionHandler={this.props.toggleShippingModal}
              applyAction={this.props.saveShippingAddress}
              saveState={this.props.saveShippingState}
              buttonLabel="Apply"
              {...this.props}
            />
          </Modal>
        </div>
      );
    }

    return (
      <Loader size="m" />
    );
  }

  render() {
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

function mapStateToProps(state) {
  return {
    saveShippingState: _.get(state.asyncActions, 'saveShippingAddress', {}),
    shippingModalVisible: _.get(state.checkout, 'shippingModalVisible', false),
    addressesState: _.get(state.asyncActions, 'addresses', {}),
    updateAddressState: _.get(state.asyncActions, 'updateAddress', false),
    cartChangeState: _.get(state.asyncActions, 'cartChange', false),
  };
}

export default _.flowRight(
  localized,
  connect(mapStateToProps, {
    ...checkoutActions,
  })
)(Shipping);
