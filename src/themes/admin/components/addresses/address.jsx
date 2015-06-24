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
        <div><input type='checkbox' defaultChecked={address.isDefault} disabled='disabled' /> Default Address</div>
    );
    let street2 = (val) => {
      return <span><span>{val}</span><br/></span>;
    };
    let choose = '';
    if (this.props.order) {
      if (!address.isActive) choose = <a className='btn choose' onClick={this.setActiveAddress.bind(this)}>Choose</a>;
    }


    let classes = ['address'];

    if (address.isActive) classes.push('active');

    return (
      <li className={`${classes.join(' ')}`}>
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
        { choose }
      </li>
    );
  }
}

Address.propTypes = {
  address: React.PropTypes.object,
  order: React.PropTypes.object
};

export default Address;
