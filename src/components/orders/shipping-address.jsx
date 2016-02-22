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
          order={this.props.order}
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
