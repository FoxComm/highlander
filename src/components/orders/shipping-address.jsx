import React, { PropTypes } from 'react';
import { EditButton, PrimaryButton } from '../common/buttons';
import Addresses from '../addresses/addresses';
import AddressDetails from '../addresses/address-details';
import * as OrdersActions from '../../modules/orders/list';
import ContentBox from '../content-box/content-box';
import { connect } from 'react-redux';


export default class OrderShippingAddress extends React.Component {

  static propTypes = {
    order: PropTypes.object.isRequired,
    isEditing: PropTypes.bool
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      isEditing: false
    };
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

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  isAddressSelected(address) {
    return this.props.order ? address.id === this.props.order.shippingAddress.id : false;
  }

  render() {
    let address = this.props.order.shippingAddress;
    let body = null;
    let editButton = null;
    let footer = null;

    if (this.state.isEditing) {
      body = (
        <div className="fc-tableview">
          <Addresses order={this.props.order} onSelectAddress={this.onSelectAddress.bind(this)} />
        </div>
      );
      footer = (
        <footer className="fc-line-items-footer">
          <div>
            <PrimaryButton onClick={this.toggleEdit}>Done</PrimaryButton>
          </div>
        </footer>
      );
    } else {
      body = (
        <AddressDetails address={address} />
      );
      editButton = (
        <div>
          <EditButton onClick={this.toggleEdit} />
        </div>
      );
    }

    return (
      <ContentBox
        title="Shipping Address"
        actionBlock={editButton}
        className="fc-order-shipping-address">
        {body}
        {footer}
      </ContentBox>
    );
  }
}
