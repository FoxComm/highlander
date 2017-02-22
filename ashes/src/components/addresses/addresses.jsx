import React, { PropTypes } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';

import AddressBox from './address-box';
import EmptyText from '../content-box/empty-text';
import ConfirmationDialog from '../modal/confirmation-dialog';

/**
 * Address list. Requires actions which interface described in customers/address-details and address modules.
 */
const Addresses = props => {
  const content = props.isAdding || props.addresses.length ? renderContent(props) : renderEmpty();

  return (
    <div>
      {content}
      <ConfirmationDialog
        isVisible={ props.deletingId != null } /* null and undefined */
        header='Confirm'
        body='Are you sure you want to delete this address?'
        cancel='Cancel'
        confirm='Yes, Delete'
        onCancel={() => props.stopDeletingAddress(props.customerId) }
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

const renderContent = (props) => {
  return (
    <ul id="fct-customer-addresses-list" className="fc-addresses-list fc-float-list">
      {props.processContent(
        props.addresses.map((address, idx) => props.createAddressBox(address, idx, props))
      )}
    </ul>
  );
};

const renderEmpty = () => {
  return <EmptyText label="No saved addresses." />;
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
  startEditingAddress: PropTypes.func,
  deletingId: PropTypes.number
};

/*eslint "react/prop-types": 0*/

export function createAddressBox(address, idx, props) {
  const chooseAction = props.chooseAction ? () => props.chooseAction(address.id) : null;

  return (
    <AddressBox
      key={`address-${idx}`}
      address={address}
      chosen={props.selectedAddressId == address.id}
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
