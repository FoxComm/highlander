
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';
import { connect } from 'react-redux';
import { browserHistory } from 'lib/history';

// components
import EditableBlock from 'ui/editable-block';
import RadioButton from 'ui/radiobutton/radiobutton';
import { AddressDetails } from 'ui/address';
import Block from '../common/block';

// styles
import addressStyles from './../../../pages/checkout/01-shipping/address-list.css';
import profileStyles from '../profile.css';

const styles = {...addressStyles, ...profileStyles};

import { updateAddress, fetchAddresses } from 'modules/checkout';

type Props = {
  addresses: Array<any>,
  updateAddress: Function,
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
  editAddress(address) {
    browserHistory.push(`/profile/addresses/${address.id}`);
  }

  @autobind
  addAddress() {
    browserHistory.push('/profile/addresses/new');
  }

  @autobind
  selectAddressById(id) {
    this.setState({
      activeAddressId: id,
    });
  }

  renderAddresses() {
    const items = _.map(this.props.addresses, (address) => {
      const content = <AddressDetails address={address} hideName />;
      const checked = address.id === this.state.activeAddressId;
      const key = address.id;

      return (
        <li styleName="item" key={`address-radio-${key}`}>
          <RadioButton
            id={`address-radio-${key}`}
            name={`address-radio-${key}`}
            checked={checked}
            onChange={() => this.selectAddressById(address.id)}
          >
            <EditableBlock
              isEditing={false}
              styleName="item-content"
              title={address.name}
              content={content}
              editAction={() => this.editAddress(address)}
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
  connect(mapStateToProps, {updateAddress, fetchAddresses}),
  localized
)(MyShippingAddresses);
