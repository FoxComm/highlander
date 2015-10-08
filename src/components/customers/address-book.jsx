'use strict';

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import AddressBox from '../addresses/address-box';
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
    console.log(this.state.addresses);

    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-add"></i>
      </button>
    );

    let createAddressBox = (addr) => {
      let key = `cutomer-address-${ addr.id }`;
      return (
        <AddressBox key={ key }
                    address={ addr }
                    customerId={ this.props.customerId } />
      );
    };
    return (
      <ContentBox title="Address Book"
                  className="fc-customer-address-book"
                  actionBlock={ actionBlock }>

        <ul className="fc-float-list clearfix">
          {this.state.addresses.map(createAddressBox)}
        </ul>
      </ContentBox>
    );
  }
}
