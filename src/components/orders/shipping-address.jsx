import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import AddressDetails from '../addresses/address-details';
import ChooseShippingAddress from './choose-shipping-address';
import ContentBox from '../content-box/content-box';
import EditableContentBox from '../content-box/editable-content-box';
import PanelHeader from './panel-header';

export default class OrderShippingAddress extends Component {
  static propTypes = {
    isCart: PropTypes.bool.isRequired,
    order: PropTypes.object.isRequired,
    status: PropTypes.string,
    readOnly: PropTypes.bool,
  };

  static defaultProps = {
    readOnly: false,
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
    const { isCart, readOnly, status } = this.props;

    const title = <PanelHeader isCart={isCart} status={status} text="Shipping Address" />;
    const isCheckingOut = _.get(this.props, 'order.isCheckingOut', false);
    const editAction = isCheckingOut
      ? null
      : this.handleToggleEdit;

    const ShippingAddressContentBox = !readOnly && isCart ? EditableContentBox : ContentBox; 

    return (
      <ShippingAddressContentBox
        className="fc-shipping-address"
        title={title}
        indentContent={true}
        isEditing={this.state.isEditing}
        editAction={editAction}
        doneAction={this.handleToggleEdit}
        renderContent={this.renderContent}
      />
    );
  }
}
