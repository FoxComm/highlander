/** @flow */
import React, { PropTypes, Component } from 'react';
import _ from 'lodash';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import Alert from '../alerts/alert';
import ErrorAlerts from '../alerts/error-alerts';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import { PrimaryButton, Button } from '../common/buttons';
import WrapToLines from './wrap-to-lines';
import WaitAnimation from '../common/wait-animation';

import * as userActions from 'modules/user';

import styles from './css/auth.css';

// types
import type {
  LoginPayload,
  TUser,
} from 'modules/user';


type TState = {
  org: string;
  email: string;
  password: string;
};

type LoginProps = {
  current: TUser,
  authenticate: (payload: LoginPayload) => Promise<*>,
  user: {
    message: String,
  },
  authenticationState: {
    err?: any,
    inProgress?: boolean,
  },
  err: any,
  googleSignin: Function,
  isMounted: boolean,
}

/* ::`*/
@connect((state) => ({
  user: state.user,
  authenticationState: _.get(state.asyncActions, 'authenticate', {})
}), userActions)
/* ::`*/
export default class Login extends Component {
  state: TState = {
    org: '',
    email: '',
    password: '',
  };

  props: LoginProps;


  @autobind
  submitLogin() {
    const payload = _.pick(this.state, 'email', 'password', 'org');

    this.props.authenticate(payload).then(() => {
      transitionTo('home');
    });
  }

  @autobind
  onOrgChange({ target }: Object) {
    this.setState({ org: target.value });
  }

  @autobind
  onEmailChange({ target }: Object) {
    this.setState({ email: target.value });
  }

  @autobind
  onPasswordChange({ target }: Object) {
    this.setState({ password: target.value });
  }

  @autobind
  onForgotClick() {
    console.log('todo: restore password');
  }

  @autobind
  onGoogleSignIn() {
    this.props.googleSignin();
  }

  get iForgot() {
    return <a onClick={this.onForgotClick} styleName="forgot-link">i forgot</a>;
  }

  get infoMessage() {
    const { message } = this.props.user;
    if (!message) return null;
    return <Alert type="success">{message}</Alert>;
  }

  get errorMessage() {
    const err = this.props.authenticationState.err;
    if (!err) return null;
    return <ErrorAlerts error={err} />;
  }

  get content() {
    if (!this.props.isMounted) {
      return <WaitAnimation />;
    }

    const { org, email, password } = this.state;

    return (
      <div styleName="content">
        {this.infoMessage}
        <Button
          type="button"
          styleName="google-button"
          icon="google"
          onClick={this.onGoogleSignIn}
        >
          Sign In with Google
        </Button>
        <Form styleName="form" onSubmit={this.submitLogin}>
          <WrapToLines styleName="or-line">or</WrapToLines>
          {this.errorMessage}
          <FormField label="Organization" required>
            <input onChange={this.onOrgChange} value={org} type="text" className="fc-input" />
          </FormField>
          <FormField label="Email" required>
            <input onChange={this.onEmailChange} value={email} type="text" className="fc-input" />
          </FormField>
          <FormField label="Password" labelAtRight={this.iForgot} required>
            <input onChange={this.onPasswordChange} value={password} type="password" className="fc-input" />
          </FormField>
          <PrimaryButton
            styleName="submit-button"
            type="submit"
            isLoading={this.props.authenticationState.inProgress}>
            Sign In
          </PrimaryButton>
        </Form>
      </div>
    );
  }

  render() {
    return (
      <div styleName="main">
        <div className="fc-auth__title">Sign In</div>
        {this.content}
      </div>
    );
  }
}
