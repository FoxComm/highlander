// @flow weak

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';
import { connect } from 'react-redux';
import { browserHistory } from 'lib/history';
import { api as foxApi } from 'lib/api';

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
  updateAddress: Function,
  updateShippingAddress: Function,
  deleteAddress: (id: number) => Promise,
  restoreAddress: (id: number) => Promise,
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
          const defaultAddress = _.find(this.props.addresses, { isDefault: true });
          const selected = defaultAddress ? defaultAddress.id : this.props.addresses[0].id;
          this.handleAddresses(selected, false);
        }
      });

  }

  componentWillUpdate(nextProps: Props, nextState: State) {
    const selectedAddress = _.find(nextProps.addresses, { id: nextState.activeAddressId });
    if (nextProps.addresses.length > 0 && !selectedAddress) {
      const defaultAddress = _.find(nextProps.addresses, { isDefault: true });
      const selected = defaultAddress ? defaultAddress.id : nextProps.addresses[0].id;
      this.handleAddresses(selected, false);
    }
  }

  @autobind
  addAddress() {
    browserHistory.push('/profile/addresses/new');
  }

  @autobind
  handleAddresses(addressId, deleted) {
    const newShippingAddress = _.find(this.props.addresses, { 'id': addressId });

    if (deleted) {
      this.props.markAddressAsDefault(addressId);
      foxApi.addresses.setAsDefault(addressId);
    } else {
      if (!newShippingAddress.isDefault) {
        newShippingAddress.isDefault = true;
        this.props.updateAddress(newShippingAddress, addressId);
      }
    }
    this.setCartShippingAddress(newShippingAddress);
    this.selectAddressById(addressId);
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
            this.handleAddresses(newDefault, true);
          }
          else {
            foxApi.cart.removeShippingAddress();
          }
        }
      });
  }

  @autobind
  setCartShippingAddress(addr) {
    if (this.props.addresses.length === 1) {
      this.props.saveShippingAddress(addr.id);
    } else {
      this.props.updateShippingAddress(addr);
    }
  }

  @autobind
  selectAddressById(id) {
    this.setState({
      activeAddressId: id,
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
            <div styleName="link" onClick={() => this.deleteAddress(address.id, address.isDefault)}>{props.t('REMOVE')}</div>
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
            onChange={() => this.handleAddresses(address.id, false)}
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
