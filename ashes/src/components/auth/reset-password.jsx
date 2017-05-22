// @flow

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';

import Form from 'components/forms/form';
import FormField from 'components/forms/formfield';
import ErrorAlerts from 'components/alerts/error-alerts';
import { PrimaryButton } from 'components/core/button';
import PasswordInput from 'components/forms/password-input';
import WaitAnimation from 'components/common/wait-animation';
import Link from 'components/link/link';

import type { TResetPayload } from 'modules/user';
import * as userActions from 'modules/user';

import s from './css/auth.css';

type State = {
  email: string,
  password1: string,
  password2: string,
  dataSent: boolean,
};

type Props = {
  resetState: {
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
    resetState: _.get(state.asyncActions, 'resetPassword', {})
  };
}

function sanitize(): string {
  return 'Passwords do not match or security code is invalid.';
}

class ResetPassword extends Component {
  props: Props;

  state: State = {
    email: '',
    password1: '',
    password2: '',
    dataSent: false,
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
    const payload = { newPassword: this.state.password1, code: this.token };
    this.props.resetPassword(payload).then(() => {
      this.setState({ dataSent: true });
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

  @autobind
  goBack() {
    transitionTo('login');
  }

  get errorMessage() {
    const err = this.props.resetState.err;
    if (!err) return null;
    return <ErrorAlerts error={err} sanitizeError={sanitize} />;
  }

  get confirmation() {
    return (
      <div className={s.main}>
        <div className={s.message}>
          Your password was successfully reset.
        </div>
        <div className={s['button-block']}>
          <Link
            to='login'
            className={s['back-button']}
          >
            Back to Login
          </Link>
        </div>
      </div>
    );
  }

  get content() {
    if (!this.props.isMounted) {
      return <WaitAnimation />;
    }

    return (
      <div className={s.main}>
        <Form className={s.form} onSubmit={this.handleSubmit}>
          {this.errorMessage}
          <FormField className={s['signup-email']} label="Email">
            <input
              name="email"
              value={this.email}
              onChange={this.handleInputChange}
              type="email"
              disabled
              className="fc-input"
            />
          </FormField>
          <FormField className={s.password} label="New Password">
            <PasswordInput
              name="password1"
              onChange={this.handleInputChange}
              value={this.state.password1}
              type="password"
              required
              className="fc-input"
            />
          </FormField>
          <FormField className={s.password} label="Confirm Password" validator={this.validatePassword2}>
            <PasswordInput
              name="password2"
              onChange={this.handleInputChange}
              value={this.state.password2}
              type="password"
              required
              className="fc-input"
            />
          </FormField>
          <div className={s['button-block']}>
            <PrimaryButton
              className={s['submit-button']}
              type="submit"
              isLoading={this.props.resetState.inProgress}
            >
              Reset Password
            </PrimaryButton>
            <Link
              to='login'
              className={s['back-button']}
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
      <div className={s.main}>
        <div className="fc-auth__title">Reset Password</div>
        {!this.state.dataSent ? this.content : this.confirmation}
      </div>
    );
  }
}

export default connect(mapStateToProps, userActions)(ResetPassword);
