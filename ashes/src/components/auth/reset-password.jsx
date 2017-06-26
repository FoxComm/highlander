// @flow

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { transitionTo, transitionToLazy } from 'browserHistory';

import Form from 'components/forms/form';
import FormField from 'components/forms/formfield';
import { ApiErrors } from 'components/utils/errors';
import { PrimaryButton } from 'components/core/button';
import PasswordInput from 'components/forms/password-input';
import { Link } from 'components/link';
import TextInput from 'components/core/text-input';

import * as userActions from 'modules/user';

import s from './css/auth.css';

import type { TResetPayload } from 'modules/user';

type State = {
  email: string,
  password1: string,
  password2: string,
};

type Props = {
  resetState: AsyncState,
  requestPasswordReset: (email: string) => Promise<*>,
  resetPassword: (payload: TResetPayload) => Promise<*>,
  isMounted: boolean,
  location: Location,
};

function mapStateToProps(state) {
  return {
    resetState: _.get(state.asyncActions, 'resetPassword', {}),
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
    this.props.resetPassword(payload).then(transitionToLazy('home'));
  }

  @autobind
  handleInputChange({ target }: SyntheticInputEvent) {
    this.setState({
      [target.name]: target.value,
    });
  }

  @autobind
  validatePassword2(value: string) {
    if (this.state.password1 != value) {
      return 'Passwords does not match';
    }
  }

  @autobind
  goBack() {
    transitionTo('login');
  }

  get errorMessage(): ?Element<*> {
    const err = this.props.resetState.err;

    if (!err) return null;

    return <ApiErrors error={err} sanitizeError={sanitize} />;
  }

  get content(): Element<*> {
    return (
      <div className={s.main}>
        <Form className={s.form} onSubmit={this.handleSubmit}>
          {this.errorMessage}
          <FormField className={s.signupEmail} label="Email">
            <TextInput name="email" value={this.email} type="email" disabled className="fc-input" />
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
          <div className={s.buttonBlock}>
            <PrimaryButton className={s.submitButton} type="submit" isLoading={this.props.resetState.inProgress}>
              Reset Password
            </PrimaryButton>
            <Link to="login" className={s.backButton}>
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
        {this.content}
      </div>
    );
  }
}

export default connect(mapStateToProps, userActions)(ResetPassword);
