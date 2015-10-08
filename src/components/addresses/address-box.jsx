'use strict';

import React from 'react';
import AddressDetails from './address-details.jsx';

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

    return (
      <li className="fc-address">
        <div className="fc-address-controls fc-grid">
          <div className="fc-col-2-3">
            { isDefault }
          </div>
          <div className="fc-col-1-3">
            <button className="fc-btn icon-trash"></button>
            <button className="fc-btn icon-edit"></button>
          </div>
        </div>
        <div>
            <AddressDetails address={address} />
        </div>
      </li>
    );
  }
}
