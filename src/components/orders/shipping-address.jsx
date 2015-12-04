
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import { EditButton, PrimaryButton, AddButton, Button } from '../common/buttons';
import Addresses from '../addresses/addresses';
import AddressBox from '../addresses/address-box';
import AddressDetails from '../addresses/address-details';
import * as OrdersActions from '../../modules/orders/list';
import EditableContentBox from '../content-box/editable-content-box';
import { connect } from 'react-redux';
import * as AddressesActions from '../../modules/customers/addresses';
import * as ShippingAddressesActions from '../../modules/orders/shipping-addresses';
import AddressForm from '../addresses/address-form/modal';
import ConfirmationDialog from '../modal/confirmation-dialog';
import ErrorAlerts from '../alerts/error-alerts';

const addressTypes = ShippingAddressesActions.addressTypes;

function mapStateToProps(state, props) {
  const addressesState = state.customers.addresses[props.order.customer.id];
  const selectedProps = {
    customerId: props.order.customer.id,
    address: props.order.shippingAddress
  };

  return {
    ...addressesState,
    ...selectedProps,
    ...state.orders.shippingAddresses
  };
}

/*eslint "react/prop-types": 0*/

@connect(mapStateToProps, {
  ...AddressesActions,
  ...ShippingAddressesActions
})
export default class OrderShippingAddress extends React.Component {

  static propTypes = {
    order: PropTypes.object.isRequired
  };

  componentDidMount() {
    this.props.fetchAddresses(this.props.customerId);
  }

  get selectedShippingAddress() {
    const props = this.props;
    const address = props.order.shippingAddress;

    let actionBlock = null;

    if (props.order.orderStatus === 'cart') {
      actionBlock = (
        <Button onClick={() => props.startDeletingAddress(address.id, addressTypes.SHIPPING)}>
          Remove shipping address
        </Button>
      );
    }

    if (address) {
      return (
        <div>
          <h3 className="fc-shipping-address-sub-title">Chosen Address</h3>
          <ul className="fc-addresses-list">
            <AddressBox
              address={address}
              chosen={true}
              checkboxLabel={null}
              editAction={() => props.startEditingAddress(address.id, addressTypes.SHIPPING)}
              actionBlock={ actionBlock }
            />
          </ul>
        </div>
      );
    }
  }

  get editContent() {
    const props = this.props;

    const deletingId = props.deletingAddress &&
      props.deletingAddress.type === addressTypes.CUSTOMER &&
      props.deletingAddress.id || null;

    return (
      <div>
        { this.selectedShippingAddress }
        <header className="fc-shipping-address-header">
          <h3 className="fc-shipping-address-sub-title">Address Book</h3>
          <AddButton onClick={() => this.props.startAddingAddress()} />
        </header>
        <div className="fc-tableview">
          <Addresses
            {...props}
            deletingId={ deletingId }
            chooseAction={addressId => props.chooseAddress(props.order.referenceNumber, addressId)}
            />
        </div>
        <ConfirmationDialog
          isVisible={ !!(props.deletingAddress && props.deletingAddress.type === addressTypes.SHIPPING) }
          header='Confirm'
          body='Are you sure you want to remove shipping address from order?'
          cancel='Cancel'
          confirm='Yes, Delete'
          cancelAction={() => props.stopDeletingAddress() }
          confirmAction={() => {
            props.stopDeletingAddress();
            props.deleteShippingAddress(props.order.referenceNumber);
          }} />
      </div>
    );
  }

  get viewContent() {
    if (this.props.address) {
      return <AddressDetails address={this.props.address} />;
    } else {
      return <div className="fc-content-box-notice">No shipping method applied.</div>;
    }
  }

  @autobind
  renderContent(isEditing) {
    return (
      <div>
        <ErrorAlerts error={ this.props.err } closeAction={ this.props.spliceError } />
        { isEditing ? this.editContent : this.viewContent }
      </div>
    );
  }

  render() {
    const props = this.props;

    const editingAddress = props.editingAddress &&
      (
        props.editingAddress.type === addressTypes.CUSTOMER ?
          _.findWhere(props.addresses, {id: props.editingAddress.id}) : props.order.shippingAddress
      );

    let submitAction = null;
    let saveTitle = 'Save and Choose';
    let onSaved = addressId => props.chooseAddress(props.order.referenceNumber, addressId);

    if (props.editingAddress && props.editingAddress.type === addressTypes.SHIPPING) {
      submitAction = formData => {
        return props.patchShippingAddress(props.order.referenceNumber, formData);
      };
      saveTitle = 'Save';
      onSaved = null;
    }

    return (
      <div>
        <EditableContentBox
          className="fc-shipping-address"
          title="Shipping Address"
          indentContent={true}
          isEditing={props.isEditing}
          editAction={props.startEditing}
          doneAction={props.stopEditing}
          renderContent={ this.renderContent }
        />

        <AddressForm
          isVisible={ props.editingAddress != null }
          address={ editingAddress }
          submitAction={ submitAction }
          closeAction={ () => props.stopAddingOrEditingAddress() }
          customerId={ props.customerId }
          saveTitle={saveTitle}
          onSaved={onSaved}
        />
      </div>
    );
  }
}
