
import _ from 'lodash';
import React, { PropTypes } from 'react';
import AddressBox from './address-box';
import { connect } from 'react-redux';
import ConfirmationDialog from '../modal/confirmation-dialog';
import * as CustomerAddressesActions from '../../modules/customers/addresses';

/**
 * Address list. Requires actions from customers/address module.
 */
const Addresses = props => {
  const content = props.processContent(
    props.addresses.map((address, idx) => props.createAddressBox(address, idx, props))
  );

  return (
    <div>
      <ul className="fc-addresses-list fc-float-list">
        {content}
      </ul>
      <ConfirmationDialog
        isVisible={ props.deletingId != null } /* null and undefined */
        header='Confirm'
        body='Are you sure you want to delete this address?'
        cancel='Cancel'
        confirm='Yes, Delete'
        cancelAction={() => props.stopDeletingAddress(props.customerId) }
        confirmAction={() => {
            props.deleteAddress(props.deletingId);
            props.onDeleteAddress && props.onDeleteAddress(props.deletingId);
          }} />
    </div>
  );
};

Addresses.propTypes = {
  customerId: PropTypes.number.isRequired,
  addresses: PropTypes.array,
  fetchAddresses: PropTypes.func,
  chooseAction: PropTypes.func,
  onDeleteAddress: PropTypes.func,
  isAddressSelected: PropTypes.func,
  createAddressBox: PropTypes.func,
  processContent: PropTypes.func,
  startDeletingAddress: PropTypes.func,
  stopDeletingAddress: PropTypes.func,
  startEditingAddress: PropTypes.func
};


Addresses.defaultProps = {
  addresses: [],
  createAddressBox: (address, idx, props) => {
    return (
      <AddressBox key={`address-${idx}`}
                  address={address}
                  choosen={props.isAddressSelected ? props.isAddressSelected(address) : false}
                  editAction={address => props.startEditingAddress(props.customerId, address.id)}
                  chooseAction={address => props.chooseAction(props.customerId, address.id)}
                  deleteAction={address => props.startDeletingAddress(props.customerId, address.id)}
      />
    );
  },
  processContent: _.identity
};

export default Addresses;
