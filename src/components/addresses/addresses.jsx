'use strict';

import React, { PropTypes } from 'react';
import Address from './address';
import AddressForm from './address-form.jsx';
import AddressStore from '../../stores/addresses';
import { dispatch } from '../../lib/dispatcher';

export default class AddressBook extends React.Component {

  static propTypes = {
    order: PropTypes.object,
    onSelectAddress: PropTypes.func,
    params: PropTypes.shape({
      customer: PropTypes.string
    })
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      addresses: [],
      customerId: null
    };
  }

  componentDidMount() {
    let customerId;
    AddressStore.listenToEvent('change', this);
    if (this.props.order) {
      customerId = this.props.order.customer.id;
    } else if (this.props.params && this.props.params.customer) {
      customerId = this.props.params.customer;
    } else {
      throw new Error('customer not provided to AddressBook');
    }

    this.setState({customerId});
    AddressStore.fetch(customerId);
  }

  componentWillUnmount() {
    AddressStore.stopListeningToEvent('change', this);
  }

  onChangeAddressStore(customerId, addresses) {
    if (customerId === this.state.customerId) {
      this.setState({addresses});
    }
  }

  addNew() {
    dispatch('toggleModal', <AddressForm customerId={this.state.customerId} order={this.props.order}/>);
  }

  render() {
    let
      addresses = this.state.addresses,
      order = this.props.order || null;

    return (
      <div className="fc-addresses">
        <header>
          <div className="fc-addresses-title">Address Book</div>
          <button className="fc-btn icon-add" onClick={this.addNew.bind(this)}></button>
        </header>
        <ul className="fc-addresses-list">
          {addresses.map((address, idx) => {
            return (
              <Address key={`${idx}-${address.id}`}
                address={address}
                order={order}
                onSelectAddress={this.props.onSelectAddress}
                onDeleteAddress={this.props.onDeleteAddress}
                customerId={this.state.customerId}
              />
            );
          })}
        </ul>
      </div>
    );
  }
}
