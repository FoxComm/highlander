'use strict';

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import AddressStore from '../../stores/addresses';

export default class CustomerAddressBook extends React.Component {
  static propTypes = {
    customerId: PropTypes.number.isRequired
  }

  constructor(props, context) {
    super(props, context);
    this.state = {
      addresses: []
    };
  }

  componentDidMount() {
    AddressStore.listenToEvent('change', this);
    AddressStore.fetch(this.props.customerId);
  }

  componentWillUnmount() {
    AddressStore.stopListeningToEvent('change', this);
  }

  onChangeAddressStore(customerId, addresses) {
    if (customerId != this.props.customerId) return;
    this.setState({
      addresses: addresses
    });
  }

  render() {
    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-add"></i>
      </button>
    );

    let createAddressBox = (addr) => {
        return (
          <li key={addr.id}>{addr.name}</li>
        );
    };
    return (
      <ContentBox title="Address Book"
                  className="fc-customer-address-book"
                  actionBlock={ actionBlock }>

        <ul className="fc-float-list">
          {this.state.addresses.map(createAddressBox)}
        </ul>
      </ContentBox>
    );
  }
}
