
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { EditButton, PrimaryButton, AddButton } from '../common/buttons';
import Addresses from '../addresses/addresses';
import AddressDetails from '../addresses/address-details';
import * as OrdersActions from '../../modules/orders/list';
import EditableContentBox from '../content-box/editable-content-box';
import { connect } from 'react-redux';
import * as AddressesActions from '../../modules/addresses';
import * as ShippingAddressesActions from '../../modules/orders/shipping-addresses';
import AddressForm from '../addresses/address-form';

function mapStateToProps(state, props) {
  const addressesState = state.addresses[props.order.customer.id];
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
      <div>
        <header className="fc-shipping-address-header">
          <h3>Address Book</h3>
          <AddButton onClick={() => this.props.startAddingAddress()}></AddButton>
        </header>
        <div className="fc-tableview">
          <Addresses
            {...this.props}
            onSelectAddress={this.onSelectAddress.bind(this)}
          />
        </div>
      </div>
    );
  }

  get viewContent() {
    return <AddressDetails address={this.props.address} />;
  }

  render() {
    const props = this.props;

    return (
      <div>
        <EditableContentBox
          className='fc-shipping-address'
          title="Shipping Address"
          isTable={false}
          isEditing={props.isEditing}
          editAction={props.startEditing}
          doneAction={props.stopEditing}
          renderFooter={null}
          renderContent={isEditing => isEditing ? this.editContent : this.viewContent}
        />

        <AddressForm
          isVisible={props.editingId != null}
          address={_.findWhere(props.addresses, {id: props.editingId})}
          closeAction={() => props.stopAddingOrEditingAddress(props.customerId)}
          customerId={props.customerId}
        />
      </div>
    );
  }
}
