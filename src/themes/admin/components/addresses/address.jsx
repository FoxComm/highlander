'use strict';

import React from 'react';

class Address extends React.Component {
  render() {
    let address = this.props.address;

    let isDefault = (val) => {
      return (
        <div><input type='checkbox' checked={val} /> Default Address</div>
      );
    };
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
          { address.isDefault ? isDefault(address.isDefault) : '' }
          <p><strong>{address.name}</strong></p>
          <p>
            <span>{address.street1}</span><br />
            { address.street2 ? street2(address.street2) : '' }
            <span>{address.city}</span>, <span>{address.state}</span> <span>{address.zip}</span><br />
            <span>{address.country}</span>
          </p>
        </div>
        <a className='btn choose'>Choose</a>
      </li>
    );
  }
}

Address.propTypes = {
  address: React.PropTypes.object
};

export default Address;
