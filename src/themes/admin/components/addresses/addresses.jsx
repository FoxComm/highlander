'use strict';

import React from 'react';
import Address from './address';

import AddressStore from './store';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const changeEvent = 'change-address-store';

export default class AddressBook extends React.Component {
    constructor(props) {
    super(props);
    this.onChangeAddressStore = this.onChangeAddressStore.bind(this);
    this.state = {
      addresses: AddressStore.getState()
    };
  }

  componentDidMount() {
    listenTo(changeEvent, this);
    AddressStore.fetch();
  }

  componentWillUnmount() {
    stopListeningTo(changeEvent, this);
  }

  onChangeAddressStore() {
    this.setState({addresses: AddressStore.getState()});
  }

  render() {
    let addresses = this.state.addresses;

    return (
      <ul className='addresses'>
        {addresses.map((address, idx) => {
          return <Address key={`${idx}-${address.id}`} address={address}/>;
        })}
      </ul>
    );
  }
}
