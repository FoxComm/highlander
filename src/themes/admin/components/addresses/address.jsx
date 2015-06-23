'use strict';

import React from 'react';
import AddressStore from './store';

class Address extends React.Component {
  setActiveAddress() {
    AddressStore.patch(this.props.address.id, {isActive: true});
  }

  render() {
    let address = this.props.address;

    let isDefault = (
        <div><input type='checkbox' defaultChecked={address.isDefault} /> Default Address</div>
    );
    let street2 = (val) => {
      return (
        <span><span>{val}</span><br/></span>
      );
    };

    let classes = 'address';

    if (address.isActive) {
      classes += ' active';
    }

    return (
      <li className={classes}>
        <div className='details'>
          { address.isDefault ? isDefault : '' }
          <p><strong>{address.name}</strong></p>
          <p>
            <span>{address.street1}</span><br />
            { address.street2 ? street2(address.street2) : '' }
            <span>{address.city}</span>, <span>{address.state}</span> <span>{address.zip}</span><br />
            <span>{address.country}</span>
          </p>
        </div>
        { address.isActive ? '' : <a className='btn choose' onClick={this.setActiveAddress.bind(this)}>Choose</a> }
      </li>
    );
  }
}

Address.propTypes = {
  address: React.PropTypes.object
};

export default Address;
