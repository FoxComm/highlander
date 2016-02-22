import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

import * as AddressActions from '../../modules/customers/addresses';

import AddressBox from '../addresses/address-box';
import TileSelector from '../tile-selector/tile-selector';

function mapStateToProps(state, props) {
  return {
    state: state.customers.addresses[props.customerId],
  };
}

function mapDispatchToProps(dispatch, props) {
  const addressActions = _.transform(AddressActions, (res, action, key) => {
    res[key] = (...args) => dispatch(action(props.customerId, ...args));
  });

  return {
    actions: addressActions,
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class ChooseShippingAddress extends Component {
  static propTypes = {
    customerId: PropTypes.number.isRequired,
    selectedAddress: PropTypes.object,

    actions: PropTypes.shape({
      fetchAddresses: PropTypes.func.isRequired,
    }).isRequired,

    state: PropTypes.shape({
      addresses: PropTypes.array,
      isFetching: PropTypes.bool,
    }).isRequired,
  };

  static defaultProps = {
    state: {},
  };

  componentDidMount() {
    this.props.actions.fetchAddresses();
  }

  get addresses() {
    return _.get(this.props, 'state.addresses', []);
  }

  get isFetching() {
    return _.get(this.props, 'state.isFetching', false);
  }

  get renderedAddressBoxes() {
    const selectedAddressId = _.get(this.props, 'selectedAddress.id');
    return this.addresses.map(a => {
      return (
        <AddressBox
          address={a}
          chosen={selectedAddressId === a.id}
          checkboxLabel={null}
          editAction={_.noop}
          actionBlock={null} />
      );
    });
  }

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
          emptyMessage="Customer's address book is empty."
          isFetching={this.isFetching}
          items={this.renderedAddressBoxes}
          title="Address Book" />
      </div>
    );
  }
}
