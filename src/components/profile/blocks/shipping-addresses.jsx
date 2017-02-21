
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
import addressStyles from './../../../pages/checkout/01-shipping/address-list.css';
import profileStyles from '../profile.css';

const styles = {...addressStyles, ...profileStyles};

import { updateAddress, fetchAddresses, deleteAddress } from 'modules/checkout';

type Props = {
  addresses: Array<any>,
  updateAddress: Function,
  deleteAddress: (id: number) => Promise,
  t: any,
};

type State = {
  addressToEdit: Object,
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
    addressToEdit: {},
    activeAddressId: void 0,
  };

  componentWillMount() {
    this.props.fetchAddresses();
    if (this.props.addresses.length >= 1) {
      const defaultAddress = _.find(this.props.addresses, { isDefault: true });
      const selected = defaultAddress ? defaultAddress.id : this.props.addresses[0].id;
      this.selectAddressById(selected);
    }
  }

  componentWillUpdate(nextProps: Props, nextState: State) {
    const selectedAddress = _.find(nextProps.addresses, { id: nextState.activeAddressId });
    if (nextProps.addresses.length > 0 && !selectedAddress) {
      const defaultAddress = _.find(nextProps.addresses, { isDefault: true });
      const selected = defaultAddress ? defaultAddress.id : nextProps.addresses[0].id;
      this.selectAddressById(selected);
    }
  }

  @autobind
  addAddress() {
    browserHistory.push('/profile/addresses/new');
  }

  deleteAddress(addressId) {
    if (confirm('Remove address ?')) {
      this.props.deleteAddress(addressId);
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
    const items = _.map(props.addresses, (address) => {
      const content = <AddressDetails address={address} hideName />;
      const checked = address.id === this.state.activeAddressId;
      const key = address.id;

      const actionsContent = (
        <div styleName="actions-block">
          <Link styleName="link" to={`/profile/addresses/${address.id}`}>{props.t('EDIT')}</Link>
          &nbsp;|&nbsp;
          <div styleName="link" onClick={() => this.deleteAddress(address.id)}>{props.t('REMOVE')}</div>
        </div>
      );

      return (
        <li styleName="item" key={`address-radio-${key}`}>
          <RadioButton
            id={`address-radio-${key}`}
            name={`address-radio-${key}`}
            checked={checked}
            onChange={() => this.selectAddressById(address.id)}
          >
            <EditableBlock
              styleName="item-content"
              title={address.name}
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
  connect(mapStateToProps, {updateAddress, fetchAddresses, deleteAddress}),
  localized
)(MyShippingAddresses);
