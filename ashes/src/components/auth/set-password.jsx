// @flow

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import styles from './css/auth.css';
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';

import Form from 'components/forms/form';
import FormField from 'components/forms/formfield';
import ErrorAlerts from 'components/alerts/error-alerts';
import { PrimaryButton, Button } from 'components/core/button';
import PasswordInput from 'components/forms/password-input';
import WaitAnimation from 'components/common/wait-animation';
import Link from 'components/link/link';

import type { TResetPayload } from 'modules/user';
import * as userActions from 'modules/user';

type State = {
  email: string,
  password1: string,
  password2: string,
};

type Props = {
  signUpState: {
    err?: any,
    inProgress?: boolean,
  },
  requestPasswordReset: (email: string) => Promise<*>,
  resetPassword: (payload: TResetPayload) => Promise<*>,
  isMounted: boolean,
  location: Location,
};

function mapStateToProps(state) {
  return {
    signUpState: _.get(state.asyncActions, 'resetPassword', {})
  };
}

function sanitize(): string {
  return 'Passwords do not match or security code is invalid.';
}

class SetPassword extends Component {
  props: Props;

  state: State = {
    email: '',
    password1: '',
    password2: '',
  };

  get username(): string {
    return this.props.location.query.username;
  }

  get email(): string {
    return this.props.location.query.email;
  }

  get token(): string {
    return this.props.location.query.token;
  }

  @autobind
  handleSubmit() {
    const payload = {
      newPassword: this.state.password2,
      code: this.token,
    };
    this.props.resetPassword(payload).then(() => {
      transitionTo('login');
    });
  }

  @autobind
  handleInputChange(event: Object) {
    const { target } = event;
    this.setState({
      [target.name]: target.value,
    });
  }

  @autobind
  validatePassword2(value) {
    if (this.state.password1 != value) {
      return 'Passwords does not match';
    }
  }

  get errorMessage() {
    const err = this.props.signUpState.err;
    if (!err) return null;
    return <ErrorAlerts error={err} sanitizeError={sanitize} />;
  }

  get content() {
    if (!this.props.isMounted) {
      return <WaitAnimation />;
    }

    return (
      <div>
        <div styleName="message">
          Hey, {this.username}! You’ve been invited to create an account with
          FoxCommerce. All you need to do is choose your method
          to sign up.
        </div>
        <Form styleName="form" onSubmit={this.handleSubmit}>
          {this.errorMessage}
          <FormField styleName="signup-email" label="Email">
            <input
              name="email"
              value={this.email}
              type="email"
              disabled
              className="fc-input"
            />
          </FormField>
          <FormField styleName="password" label="Create Password">
            <PasswordInput
              name="password1"
              onChange={this.handleInputChange}
              value={this.state.password1}
              type="password"
              required
              className="fc-input"
            />
          </FormField>
          <FormField styleName="password" label="Confirm Password" validator={this.validatePassword2}>
            <PasswordInput
              name="password2"
              onChange={this.handleInputChange}
              value={this.state.password2}
              type="password"
              required
              className="fc-input"
            />
          </FormField>
          <div styleName="button-block">
            <PrimaryButton
              styleName="submit-button"
              type="submit"
              isLoading={this.props.signUpState.inProgress}
            >
              Sign Up
            </PrimaryButton>
            <Link
              to='login'
              styleName="back-button"
            >
              Back to Login
            </Link>
          </div>
        </Form>
      </div>
    );
  }

  render() {
    return (
      <div styleName="main">
        <div className="fc-auth__title">Create Account</div>
        {this.content}
      </div>
    );
  }
}

export default connect(mapStateToProps, userActions)(SetPassword);
