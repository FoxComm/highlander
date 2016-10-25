
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';

// components
import EditableBlock from 'ui/editable-block';
import EditAddress from '../address/edit-address';
import { Form } from 'ui/forms';
import Button from 'ui/buttons';
import ViewAddress from '../address/view-address';
import ErrorAlerts from 'wings/lib/ui/alerts/error-alerts';
import RadioButton from 'ui/radiobutton/radiobutton';

import { AddressKind } from 'modules/checkout';

// styles
import styles from './address-list.css';

type Props = {
  activeAddress?: number|string,
  addresses: Array<any>,
  collapsed: boolean,
  continueAction: Function,
  editAction: Function,
  updateAddress: Function,
  inProgress: boolean,
  t: any,
};

type State = {
  addressToEdit: Object,
  isEditFormActive: boolean,
  activeAddress?: number|string,
};

class AddressList extends Component {
  props: Props;

  state: State = {
    addressToEdit: {},
    activeAddress: this.props.activeAddress,
    isEditFormActive: false,
  };

  componentWillMount() {
    if (_.isEmpty(this.props.addresses)) {
      this.addAddress();
    }

    if (this.props.addresses.length == 1) {
      this.changeAddressOption(this.props.addresses[0].id);
    }
  }

  @autobind
  editAddress(address) {
    this.setState({
      addressToEdit: address,
      isEditFormActive: true,
    });
  }

  @autobind
  addAddress() {
    this.setState({
      isEditFormActive: true,
    });
  }

  @autobind
  finishEditingAddress(id) {
    this.props.updateAddress(id)
      .then(() => {
        this.setState({
          addressToEdit: {},
          isEditFormActive: false,
        });
      })
      .catch((error) => {
        this.setState({
          error,
        });
      }
    );
  }

  @autobind
  changeAddressOption(id) {
    this.setState({
      activeAddress: id,
    });
  }

  @autobind
  saveAndContinue() {
    this.props.continueAction(this.state.activeAddress);
  }

  renderAddresses() {
    const items = _.map(this.props.addresses, (address, key) => {
      const content = <ViewAddress { ...address } hideName />;
      const checked = address.id === this.state.activeAddress;

      return (
        <li styleName="item" key={`address-radio-${key}`}>
          <RadioButton
            id={`address-radio-${key}`}
            name={`address-radio-${key}`}
            checked={checked}
            onChange={() => this.changeAddressOption(address.id)}
          >
            <EditableBlock
              isEditing={!_.isEmpty(this.state.addressToEdit)}
              styleName="item-content"
              title={address.name}
              content={content}
              editAction={() => this.editAddress(address)}
            />
          </RadioButton>
        </li>
      );
    });

    return (
      <div>
        <ul styleName="list">{items}</ul>
        <button styleName="add-address-button" type="button" onClick={this.addAddress}>
          Add Address
        </button>
      </div>
    );
  }

  @autobind
  cancelEditing() {
    this.setState({
      addressToEdit: {},
      isEditFormActive: false,
    });
  }

  renderEditingForm(address) {
    const id = _.get(address, 'id');
    const title = _.isEmpty(this.state.addressToEdit) ? 'Add Address' : 'Edit Address';

    return (
      <Form onSubmit={() => this.finishEditingAddress(id)}>
        <div styleName="form-header">
          <legend styleName="legend">{title}</legend>
          <span styleName="action-link" onClick={this.cancelEditing}>Cancel</span>
        </div>
        <EditAddress {...this.props} address={address} addressKind={AddressKind.SHIPPING} />

        <ErrorAlerts error={this.state.error} />
        <div styleName="button-wrap">
          <Button isLoading={this.props.inProgress} styleName="checkout-submit" type="submit">Save & Continue</Button>
        </div>
      </Form>
    );
  }

  renderList() {
    const { t } = this.props;

    return (
      <Form onSubmit={this.saveAndContinue}>
        <legend styleName="legend">SHIPPING ADDRESS</legend>
        {this.renderAddresses()}

        <ErrorAlerts error={this.props.error} />
        <div styleName="button-wrap">
          <Button isLoading={this.props.inProgress} styleName="checkout-submit" type="submit">{t('CONTINUE')}</Button>
        </div>
      </Form>
    );
  }

  render() {
    return this.state.isEditFormActive ? this.renderEditingForm(this.state.addressToEdit) : this.renderList();
  }
}

export default localized(AddressList);
