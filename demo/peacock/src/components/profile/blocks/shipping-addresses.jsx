// @flow weak

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';
import { connect } from 'react-redux';
import { browserHistory } from 'lib/history';

// components
import { Link } from 'react-router';
import EditableBlock from 'ui/editable-block';
import RadioButton from 'ui/radiobutton/radiobutton';
import { AddressDetails } from 'ui/address';
import Block from '../common/block';

// styles
import addressStyles from 'pages/checkout/shipping/address-list.css';
import profileStyles from '../profile.css';

import type { Address } from 'types/address';

const styles = {...addressStyles, ...profileStyles};

import * as checkoutActions from 'modules/checkout';

type Props = {
  fetchAddresses: () => Promise,
  addresses: Array<Address>,
  updateAddress: (address: Address, id?: number) => Promise,
  deleteAddress: (id: number) => Promise,
  restoreAddress: (id: number) => Promise,
  markAddressAsDefault: (id: number) => Promise,
  updateShippingAddress: (address: Address) => Promise,
  saveShippingAddress: (id: number) => Promise,
  removeShippingAddress: Function,
  t: any,
};

type State = {
  activeAddressId?: number|string,
};

function mapStateToProps(state) {
  return {
    addresses: state.checkout.addresses,
  };
}

class MyShippingAddresses extends Component {
  props: Props;

  state: State = {
    activeAddressId: void 0,
  };

  componentWillMount() {
    this.props.fetchAddresses()
      .then(() => {
        if (this.props.addresses.length >= 1) {
          this.selectAddrOnLoad(this.props.addresses);
        }
      });
  }

  componentWillUpdate(nextProps: Props, nextState: State) {
    const selectedAddress = _.find(nextProps.addresses, { id: nextState.activeAddressId });
    if (nextProps.addresses.length > 0 && !selectedAddress) {
      this.selectAddrOnLoad(nextProps.addresses);
    }
  }

  @autobind
  selectAddrOnLoad(addresses) {
    const defaultAddress = _.find(addresses, { isDefault: true });
    const selected = defaultAddress ? defaultAddress.id : addresses[0].id;
    this.selectAddressById(selected, false);
  }

  @autobind
  addAddress() {
    browserHistory.push('/profile/addresses/new');
  }

  @autobind
  selectAddressById(addressId, deleted) {
    const newShippingAddress = _.find(this.props.addresses, { id: addressId });

    if (deleted) {
      this.props.markAddressAsDefault(addressId);
    } else {
      if (newShippingAddress && !newShippingAddress.isDefault) {
        newShippingAddress.isDefault = true;
        this.props.updateAddress(newShippingAddress, addressId);
      }
    }
    this.props.saveShippingAddress(addressId);
    this.setState({
      activeAddressId: addressId,
    });
  }

  @autobind
  deleteAddress(id, isDefault) {
    this.props.deleteAddress(id)
      .then(() => {
        if (isDefault) {
          let newDefault;
          this.props.addresses.some((address) => {
            if (!address.isDeleted) {
              newDefault = address.id;
              return true;
            }
          });
          if (newDefault) {
            this.selectAddressById(newDefault, true);
          } else {
            this.props.removeShippingAddress();
          }
        }
      });
  }
  renderAddresses() {
    const { props } = this;
    const items = _.map(props.addresses, (address: Address) => {
      const contentAttrs = address.isDeleted ? {className: styles['deleted-content']} : {};
      const content = <AddressDetails address={address} hideName {...contentAttrs} />;
      const checked = address.id === this.state.activeAddressId;
      const key = address.id;

      let actionsContent;
      let title;

      if (address.isDeleted) {
        actionsContent = (
          <div styleName="actions-block">
            <div styleName="link" onClick={() => this.props.restoreAddress(address.id)}>{props.t('RESTORE')}</div>
          </div>
        );
        title = <span styleName="deleted-content">{address.name}</span>;
      } else {
        actionsContent = (
          <div styleName="actions-block">
            <Link styleName="link" to={`/profile/addresses/${address.id}`}>{props.t('EDIT')}</Link>
            &nbsp;|&nbsp;
            <div styleName="link" onClick={() => this.deleteAddress(address.id, address.isDefault)}>
              {props.t('REMOVE')}
            </div>
          </div>
        );
        title = address.name;
      }

      return (
        <li styleName="item" key={`address-radio-${key}`}>
          <RadioButton
            id={`address-radio-${key}`}
            name={`address-radio-${key}`}
            checked={checked}
            disabled={address.isDeleted}
            onChange={() => this.selectAddressById(address.id, false)}
          >
            <EditableBlock
              styleName="item-content"
              title={title}
              content={content}
              actionsContent={actionsContent}
            />
          </RadioButton>
        </li>
      );
    });

    return (
      <div>
        <ul styleName="list">{items}</ul>
        <div styleName="buttons-footer">
          <button styleName="link-button" type="button" onClick={this.addAddress}>
            Add Address
          </button>
        </div>
      </div>
    );
  }

  render() {
    return (
      <Block title="My Shipping Addresses">
        {this.renderAddresses()}
      </Block>
    );
  }
}

export default _.flowRight(
  connect(mapStateToProps, {...checkoutActions}),
  localized
)(MyShippingAddresses);
