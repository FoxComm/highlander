
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { EditButton, PrimaryButton } from '../common/buttons';
import Addresses from '../addresses/addresses';
import AddressDetails from '../addresses/address-details';
import * as OrdersActions from '../../modules/orders/list';
import EditableContentBox from '../content-box/editable-content-box';
import { connect } from 'react-redux';
import { createSelector } from 'reselect';
import * as CustomerAddressesActions from '../../modules/customers/addresses';

const detectIsEditing = createSelector(
  (state, props) => props.address.id,
  (state, props) => state && state.editingIds || [],
  (addressId, editingIds) => editingIds.indexOf(addressId) != -1
);

function mapStateToProps(state, props) {
  const nestedState = state.customers.addresses[props.order.customer.id];
  const selectedProps = {
    customerId: props.order.customer.id,
    address: props.order.shippingAddress
  };

  return {
    ...nestedState,
    ...selectedProps,
    isEditing: detectIsEditing(nestedState, selectedProps)
  };
}

/*eslint "react/prop-types": 0*/

@connect(mapStateToProps, CustomerAddressesActions)
export default class OrderShippingAddress extends React.Component {

  static propTypes = {
    order: PropTypes.object.isRequired
  };

  componentDidMount() {
    this.props.fetchAddresses(this.props.customerId);
  }

  onSelectAddress(address) {
    OrdersActions.setShippingAddress(this.props.order.referenceNumber, address.id);
  }

  onDeleteAddress(address) {
    if (address.id === this.props.order.shippingAddress.id) {
      OrdersActions.removeShippingAddress(this.props.order.referenceNumber);
    }
    AddressStore.delete(this.props.order.customer.id, address.id);
  }

  isAddressSelected(address) {
    return this.props.order ? address.id === this.props.order.shippingAddress.id : false;
  }

  get editContent() {
    return (
      <div className="fc-tableview">
        <Addresses
          customerId={this.props.customerId}
          addresses={ this.props.addresses }
          onSelectAddress={this.onSelectAddress.bind(this)}
        />
      </div>
    );
  }

  get viewContent() {
    return <AddressDetails address={this.props.address} />;
  }

  render() {
    const props = this.props;
    const addressId = props.address.id;

    return (
      <EditableContentBox
        className='fc-order-shipping-address'
        title="Shipping Address"
        isEditing={props.isEditing}
        editAction={() => props.startEditingAddress(props.customerId, addressId)}
        doneAction={() => props.stopEditingAddress(props.customerId, addressId)}
        renderContent={isEditing => isEditing ? this.editContent : this.viewContent}
      />
    );
  }
}
