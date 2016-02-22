import React, { Component, PropTypes } from 'react';
import _ from 'lodash';

import AddressBox from '../addresses/address-box';
import TileSelector from '../tile-selector/tile-selector';

export default class ChooseShippingAddress extends Component {
  static propTypes = {
    addresses: PropTypes.array,
    selectedAddress: PropTypes.object,
  };

  static defaultProps = {
    addresses: [],
  };

  get renderSelectedAddress() {
    if (this.props.selectedAddress) {
      return (
        <div>
          <h3 className="fc-shipping-address-sub-title">
            Chosen Address
          </h3>
          <ul className="fc-addresses-list">
            <AddressBox
              address={this.props.selectedAddress}
              chosen={true}
              checkboxLabel={null}
              editAction={_.noop}
              actionBlock={null} />
          </ul>
        </div>
      );
    }
  }

  render() {
    return (
      <div>
        {this.renderSelectedAddress}
        <TileSelector
          onAddClick={_.noop}
          items={this.props.addresses}
          title="Address Book" />
      </div>
    );
  }
}
