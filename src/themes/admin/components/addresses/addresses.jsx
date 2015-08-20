'use strict';

import React from 'react';
import Address from './address';
import NewAddress from './new_address';
import AddressStore from './store';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const cancelEvent = 'cancel-new-address';

export default class AddressBook extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      addresses: [],
      new: false,
      customerId: null
    };
  }

  componentDidMount() {
    let customerId;
    AddressStore.listenToEvent('change', this);
    listenTo(cancelEvent, this);
    if (this.props.order) {
      customerId = this.props.order.customer.id;
    } else {
      let { router } = this.context;
      customerId = router.getCurrentParams().customer;
    }

    this.setState({
      customerId: customerId
    });

    AddressStore.uriRoot = `/customers/${customerId}`;
    AddressStore.fetch();
  }

  componentWillUnmount() {
    AddressStore.stopListeningToEvent('change', this);
    stopListeningTo(cancelEvent, this);
  }

  onChangeAddressStore() {
    this.setState({
      addresses: AddressStore.getState(),
      new: false
    });
  }

  onCancelNewAddress() {
    this.setState({
      new: false
    });
  }

  addNew() {
    this.setState({
      new: true
    });
  }

  render() {
    let
      addresses = this.state.addresses,
      order = this.props.order || null;

    let innerContent = (
      <div>
        <a className='btn' onClick={this.addNew.bind(this)}>+</a>
        <ul className='addresses'>
          {addresses.map((address, idx) => {
            return <Address key={`${idx}-${address.id}`} address={address} order={order}/>;
          })}
        </ul>
      </div>
    );
    if (this.state.new) innerContent = <NewAddress order={order} customerId={this.state.customerId} />;

    return <div>{innerContent}</div>;
  }
}

AddressBook.contextTypes = {
  router: React.PropTypes.func
};

AddressBook.propTypes = {
  order: React.PropTypes.object
};
