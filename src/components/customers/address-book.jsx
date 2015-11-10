import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import AddressBox from '../addresses/address-box';
import ConfirmationDialog from '../modal/confirmation-dialog';
import { AddButton } from '../common/buttons';
import EditAddressBox from './address-edit';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as CustomerAddressesActions from '../../modules/customers/addresses';


@connect((state, props) => ({
  ...state.customers.addresses[props.customerId]
}), CustomerAddressesActions)
export default class CustomerAddressBook extends React.Component {

  static propTypes = {
    customerId: PropTypes.number.isRequired,
    fetchAddresses: PropTypes.func,
    newAddress: PropTypes.func,
    addresses: PropTypes.array
  }

  componentDidMount() {
    const customer = this.props.customerId;

    this.props.fetchAddresses(customer);
  }

  @autobind
  onAddClick() {
    console.debug("onAddClick");
    const customer = this.props.customerId;
    this.props.newAddress(customer);
  }

  render() {
    const actionBlock = (
      <AddButton onClick={this.onAddClick}/>
    );
    let createAddressBox = (addr) => {
      let key = `customer-address-${ addr.id }`;
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

        <ul className="fc-float-list">
          {(this.props.addresses && this.props.addresses.map(createAddressBox))}
          {(this.props.newAddressCard && <EditAddressBox customerId={ this.props.customerId }
                                                         form={ this.props.newAddressCard }
                                                         onCancel={ this.onAddingCancel }
                                                         onSubmit={ this.onSubmitNewForm }
                                                         onChange={ this.onChangeNewFormValue }/>)}
        </ul>
        <ConfirmationDialog
          isVisible={ this.props.deletingId != null } /* null and undefined */
          header='Confirm'
          body='Are you sure you want to delete this address?'
          cancel='Cancel'
          confirm='Yes, Delete'
          cancelAction={ this.onDeleteCancel }
          confirmAction={ this.onDeleteConfirm } />
      </ContentBox>
    );
  }
}
