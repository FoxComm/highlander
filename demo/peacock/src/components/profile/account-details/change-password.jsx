// @flow

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import ShowHidePassword from 'ui/forms/show-hide-password';
import { FormField } from 'ui/forms';
import CheckoutForm from 'pages/checkout/checkout-form';

import * as actions from 'modules/profile';

// types
import type { AsyncStatus } from 'types/async-actions';
import type { AccountDetailsProps } from 'types/profile';

import styles from '../profile.css';

type Props = AccountDetailsProps & {
  fetchAccount: () => Promise<*>,
  changePassword: (oldPassword: string, newPassword: string) => Promise<*>,
  updateState: AsyncStatus,
};

type State = {
  currentPassword: string,
  newPassword1: string,
  newPassword2: string,
  error: any,
};

class ChangePassword extends Component {
  props: Props;

  state: State = {
    currentPassword: '',
    newPassword1: '',
    newPassword2: '',
    error: null,
  };

  @autobind
  clearState() {
    this.setState({
      currentPassword: '',
      newPassword1: '',
      newPassword2: '',
      error: null,
    });
  }

  @autobind
  handleCancel() {
    this.clearState();
    this.props.togglePasswordModal();
    this.props.clearPasswordErrors();
  }

  @autobind
  handlePasswordChange(event) {
    const { target } = event;
    this.setState({
      [target.name]: target.value,
      error: null,
    });
  }

  @autobind
  handleSave() {
    const { newPassword1, newPassword2, currentPassword } = this.state;

    if (newPassword1 != newPassword2) {
      return Promise.reject({
        newPassword2: 'Your passwords don\'t match.',
      });
    }

    if (currentPassword == newPassword1) {
      return Promise.reject({
        newPassword1: 'Your new password cannot be the same as your old one.',
      });
    }

    this.props.changePassword(
      currentPassword,
      newPassword1
    ).then(() => {
      this.clearState();
      this.props.togglePasswordModal();
    })
    .catch((err) => {
      this.setState({error: err});
    });
  }

  render() {
    const action = {
      title: 'Cancel',
      handler: this.handleCancel,
    };
    return (
      <CheckoutForm
        submit={this.handleSave}
        buttonLabel="Apply"
        title="Change password"
        action={action}
        error={this.props.updateState.err || this.state.error}
        inProgress={this.props.updateState.inProgress}
      >
        <FormField
          name="currentPassword"
          styleName="password-field"
        >
          <ShowHidePassword
            placeholder="Current password"
            styleName="text-input"
            value={this.state.currentPassword}
            name="currentPassword"
            onChange={this.handlePasswordChange}
            required
          />
        </FormField>
        <FormField
          name="newPassword1"
          styleName="password-field"
        >
          <ShowHidePassword
            placeholder="New password"
            styleName="text-input"
            value={this.state.newPassword1}
            name="newPassword1"
            minLength={8}
            onChange={this.handlePasswordChange}
            required
          />
        </FormField>
        <FormField
          name="newPassword2"
          styleName="password-field"
        >
          <ShowHidePassword
            placeholder="Confirm new password"
            styleName="text-input"
            value={this.state.newPassword2}
            name="newPassword2"
            minLength={8}
            onChange={this.handlePasswordChange}
            required
          />
        </FormField>
      </CheckoutForm>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    updateState: _.get(state.asyncActions, 'changePassword', {}),
  };
};

export default connect(mapStateToProps, {
  ...actions,
})(ChangePassword);
