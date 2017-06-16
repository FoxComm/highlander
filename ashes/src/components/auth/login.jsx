/** @flow */
import React, { Component } from 'react';
import _ from 'lodash';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import Alert from '../alerts/alert';
import ErrorAlerts from '../alerts/error-alerts';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import { PrimaryButton, SocialButton } from 'components/core/button';
import WrapToLines from './wrap-to-lines';
import WaitAnimation from '../common/wait-animation';

import * as userActions from 'modules/user';

import s from './css/auth.css';

// types
import type {
  LoginPayload,
  TUser,
} from 'modules/user';


type TState = {
  org: string;
  email: string;
  password: string;
  message: string;
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
};

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
    message: ''
  };

  props: LoginProps;

  componentWillReceiveProps(nextProps: LoginProps) {
    const message = _.get(nextProps, 'user.message');

    if (message) {
      this.setState({ message: message });
    }
  }

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
    // @todo: restore password
  }

  @autobind
  onGoogleSignIn() {
    this.props.googleSignin();
  }

  @autobind
  clearMessage() {
    this.setState({
      message: ''
    });
  }

  get iForgot() {
    return <a onClick={this.onForgotClick} className={s['forgot-link']}>i forgot</a>;
  }

  get infoMessage() {
    const { message } = this.state;
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
      <div className={s.content}>
        {this.infoMessage}
        <SocialButton
          type="google"
          onClick={this.onGoogleSignIn}
          fullWidth
        >
          Sign In with Google
        </SocialButton>
        <Form className={s.form} onSubmit={this.submitLogin}>
          <WrapToLines className={s['or-line']}>or</WrapToLines>
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
            onClick={this.clearMessage}
            className={s['submit-button']}
            fullWidth
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
      <div className={s.main}>
        <div className={s.title}>Sign In</div>
        {this.content}
      </div>
    );
  }
}
