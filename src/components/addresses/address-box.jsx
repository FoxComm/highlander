'use strict';

import React from 'react';
import AddressDetails from './address-details.jsx';
import ItemCardContainer from '../item-card-container/item-card-container';

export default class AddressBox extends React.Component {

  static propTypes = {
    address: React.PropTypes.object,
    customerId: React.PropTypes.number.isRequired,
  };

  constructor(props, context) {
    super(props, context);
  }

  render() {
    let address = this.props.address;

    let isDefault = (
        <label className="fc-address-default">
          <input type="checkbox" defaultChecked={address.isDefault} />
          <span>Default shipping address</span>
        </label>
    );

    let buttons = (
      <div>
        <button className="fc-btn icon-trash"></button>
        <button className="fc-btn icon-edit"></button>
      </div>
    );

    return (
      <ItemCardContainer className="fc-customer-address"
                         leftControls={ isDefault }
                         rightControls={ buttons }>
        <AddressDetails address={address} />
      </ItemCardContainer>
    );
  }
}
