// @flow
import _ from 'lodash';
import React, { Component } from 'react';
import type { Address } from 'types/address';
import { autobind } from 'core-decorators';
import styles from './guest-shipping.css';
import { connect } from 'react-redux';

// components
import EditAddress from 'ui/address/edit-address';
import CheckoutForm from '../checkout-form';
import { TextInput } from 'ui/inputs';
import { FormField } from 'ui/forms';

import { saveEmail } from 'modules/auth';

type Props = {
  shippingAddress?: Address,
  auth: Object,
  addShippingAddress: (address: Address) => Promise,
  updateShippingAddress: (address: Address) => Promise,
  onComplete: Function,
  saveEmail: (email: string) => Promise,
  submitInProgress: boolean,
  submitError: any,
}

type State = {
  newAddress?: Address,
  email: string,
}

function mapStateToProps(state) {
  return {
    submitInProgress: _.get(state.asyncActions, 'addShippingAddress.inProgress', false) ||
    _.get(state.asyncActions, 'updateShippingAddress.inProgress', false) ||
      _.get(state.asyncActions, 'save-email.inProgress', false),
    submitError: _.get(state.asyncActions, 'addShippingAddress.err') ||
    _.get(state.asyncActions, 'save-email.err'),
  };
}

class GuestShipping extends Component {
  props: Props;

  state: State = {
    newAddress: this.props.shippingAddress,
    email: _.get(this.props.auth.user, 'email', ''),
  };

  componentWillReceiveProps(nextProps) {
    if (nextProps.auth != this.props.auth) {
      this.setState({
        email: _.get(nextProps.auth.user, 'email', ''),
      });
    }
  }

  @autobind
  setNewAddress(address: Address) {
    this.setState({
      newAddress: address,
    });
  }

  @autobind
  saveAndContinue() {
    const { props } = this;
    const saveAction = props.shippingAddress ? props.updateShippingAddress : props.addShippingAddress;
    let actions = [
      saveAction(this.state.newAddress),
    ];
    if (_.get(this.props.auth.user, 'email') != this.state.email) {
      actions = [
        ...actions,
        this.props.saveEmail(this.state.email),
      ];
    }

    Promise.all(actions).then(this.props.onComplete);
  }

  @autobind
  handleChangeEmail(evt) {
    const value = _.get(evt.target, 'value');
    this.setState({
      email: value,
    });
  }

  render() {
    const { props } = this;

    return (
      <CheckoutForm
        submit={this.saveAndContinue}
        inProgress={props.submitInProgress}
        error={props.submitError}
      >
        <div styleName="subtitle">YOUR INFORMATION</div>
        <FormField>
          <TextInput value={this.state.email} styleName="guest-email" onChange={this.handleChangeEmail}/>
        </FormField>
        <div styleName="subtitle">SHIPPING ADDRESS</div>
        <EditAddress
          withoutDefaultCheckbox
          address={props.shippingAddress}
          onUpdate={this.setNewAddress}
        />
      </CheckoutForm>
    );
  }
}

export default connect(mapStateToProps, {
  saveEmail,
})(GuestShipping);
