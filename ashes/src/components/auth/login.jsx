/* @flow */

import React, { Component } from 'react';
import _ from 'lodash';
import { transitionTo, transitionToLazy } from 'browserHistory';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import Alert from 'components/alerts/alert';
import ErrorAlerts from 'components/alerts/error-alerts';
import Form from 'components/forms/form';
import FormField from 'components/forms/formfield';
import { PrimaryButton, SocialButton } from 'components/core/button';
import WrapToLines from './wrap-to-lines';
import WaitAnimation from 'components/common/wait-animation';

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
  authenticationState: AsyncState,
  err: any,
  googleSignin: Function,
  isMounted: boolean,
};

class Login extends Component {
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

    this.props.authenticate(payload).then(transitionToLazy('home'));
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
    transitionTo('restore-password');
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
    return <a onClick={this.onForgotClick} className={s.forgotLink} >i forgot</a>;
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
          <WrapToLines className={s.orLine}>or</WrapToLines>
          {this.errorMessage}
          <FormField label="Organization" required>
            <input
              onChange={this.onOrgChange}
              value={org}
              type="text"
              className="fc-input"
              autoFocus={true}
            />
          </FormField>
          <FormField label="Email" required>
            <input
              onChange={this.onEmailChange}
              value={email}
              type="text"
              className="fc-input"
            />
          </FormField>
          <FormField label="Password" labelAtRight={this.iForgot} required>
            <input
              onChange={this.onPasswordChange}
              value={password}
              type="password"
              className="fc-input"
            />
          </FormField>
          <div className={s.buttonBlock}>
            <PrimaryButton
              onClick={this.clearMessage}
              className={s.submitButton}
              fullWidth
              type="submit"
              isLoading={this.props.authenticationState.inProgress}
            >
              Sign In
            </PrimaryButton>
          </div>
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

function mapStateToProps(state) {
  return {
    user: state.user,
    authenticationState: _.get(state.asyncActions, 'authenticate', {})
  };
}

export default connect(mapStateToProps, userActions)(Login);
