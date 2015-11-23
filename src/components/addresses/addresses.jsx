
import _ from 'lodash';
import React, { PropTypes } from 'react';
import AddressBox from './address-box';
import { connect } from 'react-redux';
import ConfirmationDialog from '../modal/confirmation-dialog';
import * as CustomerAddressesActions from '../../modules/customers/addresses';

/**
 * Address list. Requires actions which interface described in customers/address and address modules.
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
            props.stopDeletingAddress();
            props.deleteAddress(props.customerId, props.deletingId)
              .then(() => {
                props.onDeleteAddress && props.onDeleteAddress(props.deletingId);
              });
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
  selectedAddressId: PropTypes.number,
  createAddressBox: PropTypes.func,
  processContent: PropTypes.func,
  startDeletingAddress: PropTypes.func,
  stopDeletingAddress: PropTypes.func,
  setAddressDefault: PropTypes.func,
  startEditingAddress: PropTypes.func
};

export function createAddressBox(address, idx, props) {
  const chooseAction = props.chooseAction ? () => props.chooseAction(address.id) : null;

  return (
    <AddressBox key={`address-${idx}`}
                address={address}
                choosen={props.selectedAddressId == address.id}
                editAction={() => props.startEditingAddress(address.id)}
                toggleDefaultAction={() => props.setAddressDefault(props.customerId, address.id, !address.isDefault)}
                chooseAction={chooseAction}
                deleteAction={() => props.startDeletingAddress(address.id)}
    />
  );
}


Addresses.defaultProps = {
  addresses: [],
  createAddressBox,
  processContent: _.identity
};

export default Addresses;
