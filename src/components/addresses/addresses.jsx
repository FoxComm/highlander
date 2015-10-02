'use strict';

import React from 'react';
import Address from './address';
import AddressForm from './address-form.jsx';
import AddressStore from '../../stores/addresses';
import { dispatch } from '../../lib/dispatcher';

export default class AddressBook extends React.Component {
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
    } else {
      let { router } = this.context;
      customerId = router.getCurrentParams().customer;
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
                customerId={this.state.customerId}
              />
            );
          })}
        </ul>
      </div>
    );
  }
}

AddressBook.contextTypes = {
  router: React.PropTypes.func
};

AddressBook.propTypes = {
  order: React.PropTypes.object,
  onSelectAddress: React.PropTypes.func
};
