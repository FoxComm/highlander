/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import localized from 'lib/i18n';

// components
import EditableBlock from 'ui/editable-block';
import { AddressDetails } from 'ui/address';
import AddressList from './address-list';

import { isGuest } from 'paragons/auth';

// styles
import styles from './shipping.css';

type Props = {
  addresses: Array<any>,
  collapsed: boolean,
  continueAction: Function,
  editAction: Function,
  fetchAddresses: Function,
  inProgress: boolean,
  isEditing: boolean,
  t: any,
  shippingAddress: Object,
  auth: ?Object,
};

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

  isGuestMode(): boolean {
    return isGuest(this.props.auth);
  }

  content() {
    const savedAddress = this.props.shippingAddress;

    if ((!_.isEmpty(savedAddress) && !this.props.isEditing)) {
      return (
        <AddressDetails address={savedAddress} styleName="savedAddress" />
      );
    }

    return (
      <AddressList { ...this.props } activeAddress={savedAddress}/>
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

export default localized(Shipping);
