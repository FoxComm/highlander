import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';

import AddressDetails from '../addresses/address-details';
import ChooseShippingAddress from './choose-shipping-address';
import EditableContentBox from '../content-box/editable-content-box';
import PanelHeader from './panel-header';

export default class OrderShippingAddress extends Component {
  static propTypes = {
    order: PropTypes.object.isRequired,
  };

  constructor(...args) {
    super(...args);

    // TODO: Move this out of React state.
    this.state = {
      isEditing: false,
    };
  }


  @autobind
  handleToggleEdit() {
    this.setState({ isEditing: !this.state.isEditing });
  }

  @autobind
  renderContent() {
    if (this.state.isEditing) {
      return (
        <ChooseShippingAddress
          customerId={this.props.order.customer.id}
          selectedAddress={this.props.order.shippingAddress} />
      );
    } else {
      const address = this.props.order.shippingAddress;
      if (address) {
        return <AddressDetails address={address} />;
      } else {
        return <div className="fc-content-box-notice">No shipping address applied.</div>;
      }
    }
  }

  render() {
    const isCart = this.props.order.status === 'cart';
    const { status } = this.props;

    const title = <PanelHeader isCart={isCart} status={status} text="Shipping Address" />;

    return (
      <EditableContentBox
        className="fc-shipping-address"
        title={title}
        indentContent={true}
        isEditing={this.state.isEditing}
        editAction={this.handleToggleEdit}
        doneAction={this.handleToggleEdit}
        renderContent={this.renderContent}
      />
    );
  }
}

//import _ from 'lodash';
//import { autobind } from 'core-decorators';
//import React, { PropTypes } from 'react';
//import { EditButton, PrimaryButton, AddButton, Button } from '../common/buttons';
//import Addresses from '../addresses/addresses';
//import AddressBox from '../addresses/address-box';
//import AddressDetails from '../addresses/address-details';
//import * as OrdersActions from '../../modules/orders/list';
//import EditableContentBox from '../content-box/editable-content-box';
//import ContentBox from '../content-box/content-box';
//import { connect } from 'react-redux';
//import * as AddressesActions from '../../modules/customers/addresses';
//import * as ShippingAddressesActions from '../../modules/orders/shipping-addresses';
//import AddressForm from '../addresses/address-form/modal';
//import ConfirmationDialog from '../modal/confirmation-dialog';
//import ErrorAlerts from '../alerts/error-alerts';
//import PanelHeader from './panel-header';

//const addressTypes = ShippingAddressesActions.addressTypes;

//function mapStateToProps(state, props) {
  //const addressesState = state.customers.addresses[props.order.customer.id];
  //const selectedProps = {
    //customerId: props.order.customer.id,
    //address: props.order.shippingAddress
  //};

  //return {
    //...addressesState,
    //...selectedProps,
    //...state.orders.shippingAddresses
  //};
//}

//[>eslint "react/prop-types": 0<]

//@connect(mapStateToProps, {
  //...AddressesActions,
  //...ShippingAddressesActions
//})
//export default class OrderShippingAddress extends React.Component {

  //static propTypes = {
    //isCart: PropTypes.bool,
    //order: PropTypes.object.isRequired,
    //status: PropTypes.string,
    //readOnly: PropTypes.bool,
  //};

  //static defaultProps = {
    //isCart: false,
    //status: '',
    //readOnly: false,
  //};

  //componentDidMount() {
    //this.props.fetchAddresses(this.props.customerId);
  //}

  //get selectedShippingAddress() {
    //const props = this.props;
    //const address = props.order.shippingAddress;

    //let actionBlock = null;

    //if (props.order.orderState === 'cart') {
      //actionBlock = (
        //<Button onClick={() => props.startDeletingAddress(address.id, addressTypes.SHIPPING)}>
          //Remove shipping address
        //</Button>
      //);
    //}

    //if (address) {
      //return (
        //<div>
          //<h3 className="fc-shipping-address-sub-title">Chosen Address</h3>
          //<ul className="fc-addresses-list">
            //<AddressBox
              //address={address}
              //chosen={true}
              //checkboxLabel={null}
              //editAction={() => props.startEditingAddress(address.id, addressTypes.SHIPPING)}
              //actionBlock={ actionBlock }
            ///>
          //</ul>
        //</div>
      //);
    //}
  //}

  //get editContent() {
    //const props = this.props;

    //const deletingId = props.deletingAddress &&
      //props.deletingAddress.type === addressTypes.CUSTOMER &&
      //props.deletingAddress.id || null;

    //return (
      //<div>
        //{ this.selectedShippingAddress }
        //<header className="fc-shipping-address-header">
          //<h3 className="fc-shipping-address-sub-title">Address Book</h3>
          //<AddButton onClick={() => this.props.startAddingAddress()} />
        //</header>
        //<div className="fc-tableview">
          //<Addresses
            //{...props}
            //deletingId={ deletingId }
            //chooseAction={addressId => props.chooseAddress(props.order.referenceNumber, addressId)}
            ///>
        //</div>
        //<ConfirmationDialog
          //isVisible={ !!(props.deletingAddress && props.deletingAddress.type === addressTypes.SHIPPING) }
          //header='Confirm'
          //body='Are you sure you want to remove shipping address from order?'
          //cancel='Cancel'
          //confirm='Yes, Delete'
          //cancelAction={() => props.stopDeletingAddress() }
          //confirmAction={() => {
            //props.stopDeletingAddress();
            //props.deleteShippingAddress(props.order.referenceNumber);
          //}} />
      //</div>
    //);
  //}

  //get viewContent() {
    //if (this.props.address) {
      //return <AddressDetails address={this.props.address} />;
    //} else {
      //return <div className="fc-content-box-notice">No shipping address applied.</div>;
    //}
  //}

  //@autobind
  //renderContent(isEditing) {
    //return (
      //<div>
        //<ErrorAlerts error={ this.props.err } closeAction={ this.props.spliceError } />
        //{ isEditing ? this.editContent : this.viewContent }
      //</div>
    //);
  //}

  //render() {
    //const props = this.props;

    //const editingAddress = props.editingAddress &&
      //(
        //props.editingAddress.type === addressTypes.CUSTOMER ?
          //_.findWhere(props.addresses, {id: props.editingAddress.id}) : props.order.shippingAddress
      //);

    //let submitAction = null;
    //let saveTitle = 'Save and Choose';
    //let onSaved = addressId => props.chooseAddress(props.order.referenceNumber, addressId);

    //if (props.editingAddress && props.editingAddress.type === addressTypes.SHIPPING) {
      //submitAction = formData => {
        //return props.patchShippingAddress(props.order.referenceNumber, formData);
      //};
      //saveTitle = 'Save';
      //onSaved = null;
    //}

    //const title = (
      //<PanelHeader
        //isCart={this.props.isCart}
        //status={this.props.status}
        //text="Shipping Address" />
    //);

    //const OrderShippingContentBox = props.readOnly || !props.isCart
      //? ContentBox
      //: EditableContentBox;

    //const isCheckingOut = _.get(props, 'order.isCheckingOut', false);
    //const editAction = isCheckingOut ? null : props.startEditing;

    //return (
      //<div>
        //<OrderShippingContentBox
          //className="fc-shipping-address"
          //title={title}
          //indentContent={true}
          //isEditing={props.isEditing}
          //editAction={editAction}
          //doneAction={props.stopEditing}
          //renderContent={ this.renderContent }
        ///>

        //<AddressForm
          //isVisible={ props.editingAddress != null }
          //address={ editingAddress }
          //submitAction={ submitAction }
          //closeAction={ () => props.stopAddingOrEditingAddress() }
          //customerId={ props.customerId }
          //saveTitle={saveTitle}
          //onSaved={onSaved}
        ///>
      //</div>
    //);
  //}
//}
