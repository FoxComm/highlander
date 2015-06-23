'use strict';

import React from 'react';
import Address from './address';
import NewAddress from './new_address';
import AddressStore from './store';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const changeEvent = 'change-address-store';
const cancelEvent = 'cancel-new-address';

export default class AddressBook extends React.Component {
    constructor(props) {
    super(props);
    this.onChangeAddressStore = this.onChangeAddressStore.bind(this);
    this.onCancelNewAddress = this.onCancelNewAddress.bind(this);
    this.state = {
      addresses: AddressStore.getState(),
      new: false
    };
  }

  componentDidMount() {
    listenTo(changeEvent, this);
    listenTo(cancelEvent, this);
    AddressStore.fetch();
  }

  componentWillUnmount() {
    stopListeningTo(changeEvent, this);
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
    let addresses = this.state.addresses;

    let innerContent = (
      <div>
        <a className='btn' onClick={this.addNew.bind(this)}>+</a>
        <ul className='addresses'>
          {addresses.map((address, idx) => {
            return <Address key={`${idx}-${address.id}`} address={address}/>;
          })}
        </ul>
      </div>
    );
    if (this.state.new) {
      innerContent = <NewAddress />;
    }
    return (
      <div>
        {innerContent}
      </div>
    );
  }
}
