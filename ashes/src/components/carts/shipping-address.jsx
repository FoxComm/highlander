import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { trackEvent } from 'lib/analytics';

import AddressDetails from 'components/addresses/address-details';
import ChooseShippingAddress from './choose-shipping-address';
import EditableContentBox from 'components/content-box/editable-content-box';
import PanelHeader from 'components/panel-header/panel-header';

export default class CartShippingAddress extends Component {
  static propTypes = {
    cart: PropTypes.object.isRequired,
    id: PropTypes.string,
    status: PropTypes.string,
    readOnly: PropTypes.bool,
  };

  static defaultProps = {
    readOnly: false,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      isEditing: false,
    };
  }

  @autobind
  handleToggleEdit() {
    const eventName = this.state.isEditing ? 'edit_shipping_address_done' : 'edit_shipping_address';
    trackEvent('Orders', eventName);
    this.setState({ isEditing: !this.state.isEditing });
  }

  @autobind
  renderContent() {
    if (this.state.isEditing) {
      return (
        <ChooseShippingAddress
          cart={this.props.cart}
          selectedAddress={this.props.cart.shippingAddress} />
      );
    } else {
      const address = this.props.cart.shippingAddress;
      if (address) {
        return <AddressDetails address={address} />;
      } else {
        return <div className="fc-content-box-notice">No shipping address applied.</div>;
      }
    }
  }

  render() {
    const { readOnly, status, id } = this.props;

    const title = <PanelHeader showStatus={true} status={status} text="Shipping Address" />;
    const isCheckingOut = _.get(this.props, 'cart.isCheckingOut', false);
    const editAction = isCheckingOut
      ? null
      : this.handleToggleEdit;

    return (
      <EditableContentBox
        id={id}
        addButtonId=""
        className="fc-shipping-address"
        title={title}
        indentContent={true}
        isEditing={this.state.isEditing}
        editButtonId="fct-edit-btn__shipping-address"
        editAction={editAction}
        doneButtonId="fct-done-btn__shipping-address"
        doneAction={this.handleToggleEdit}
        renderContent={this.renderContent}
      />
    );
  }
}
