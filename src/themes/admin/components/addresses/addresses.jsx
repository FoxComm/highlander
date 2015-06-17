'use strict';

import React from 'react';
import Address from './address';

class AddressBook extends React.Component {
  render() {
    let addresses = this.props.addresses;

    return (
      <ul className='addresses'>
        {addresses.map((address, idx) => {
          return <Address key={`${idx}-${address.id}`}/>;
        })}
      </ul>
    );
  }
}

AddressBook.propTypes = {
  addresses: React.PropTypes.array
};

AddressBook.defaultProps = {
  addresses: [{id: '1'}, {id: '1'}, {id: '1'}, {id: '1'}, {id: '1'}, {id: '1'}, {id: '1'}, {id: '1'}]
};

export default AddressBook;
