// @flow

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'lib/history';

// components
import { Link } from 'react-router';
import Button from 'ui/buttons';
import ShowHidePassword from 'ui/forms/show-hide-password';
import { Form, FormField } from 'ui/forms';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import CheckoutForm from 'pages/checkout/checkout-form';

// styles
import styles from './account-details.css';

import * as actions from 'modules/profile';

import type { AsyncStatus } from 'types/async-actions';
import type { AccountDetailsProps } from 'types/profile';

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
  props: ChangePasswordProps;

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
          styleName="name-field"
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
          styleName="name-field"
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
          styleName="name-field"
        >
          <ShowHidePassword
            placeholder="Confirm password"
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
