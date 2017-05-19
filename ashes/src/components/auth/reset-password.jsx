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
import { PrimaryButton } from 'components/core/button';
import PasswordInput from 'components/forms/password-input';
import WaitAnimation from 'components/common/wait-animation';
import Link from 'components/link/link';

import type { SignupPayload } from 'modules/user';
import * as userActions from 'modules/user';

type State = {
  email: string,
  password1: string,
  password2: string,
  dataSent: boolean,
};

type Props = {
  signUpState: {
    err?: any,
    inProgress?: boolean,
  },
  signUp: (payload: SignupPayload) => Promise<*>,
  isMounted: boolean,
  location: Location,
};

function mapStateToProps(state) {
  return {
    signUpState: _.get(state.asyncActions, 'signup', {})
  };
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
    Promise.resolve(true).then(() => {
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
    const err = this.props.signUpState.err;
    if (!err) return null;
    return <ErrorAlerts error={err} />;
  }

  get confirmation() {
    return (
      <div styleName="main">
        <div styleName="message">
          Your password was successfully reset.
        </div>
        <div styleName="button-block">
          <Link
            to='login'
            styleName="back-button"
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
      <div styleName="main">
        <Form styleName="form" onSubmit={this.handleSubmit}>
          {this.errorMessage}
          <FormField styleName="signup-email" label="Email">
            <input
              name="email"
              value={this.email}
              onChange={this.handleInputChange}
              type="email"
              disabled
              className="fc-input"
            />
          </FormField>
          <FormField styleName="password" label="New Password">
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
              Reset Password
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
        <div className="fc-auth__title">Reset Password</div>
        {!this.state.dataSent ? this.content : this.confirmation}
      </div>
    );
  }
}

export default connect(mapStateToProps, userActions)(ResetPassword);
