/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import { transitionTo, transitionToLazy } from 'browserHistory';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import Alert from 'components/core/alert';
import Form from 'components/forms/form';
import FormField from 'components/forms/formfield';
import { PrimaryButton, SocialButton } from 'components/core/button';
import WrapToLines from './wrap-to-lines';
import TextInput from 'components/core/text-input';
import { ApiErrors } from 'components/utils/errors';

import * as userActions from 'modules/user';

import s from './css/auth.css';

// types
import type { LoginPayload, TUser } from 'modules/user';

type TState = {
  org: string,
  email: string,
  password: string,
  message: string,
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
    message: '',
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
  onOrgChange(value: string) {
    this.setState({ org: value });
  }

  @autobind
  onEmailChange(value: string) {
    this.setState({ email: value });
  }

  @autobind
  onPasswordChange(value: string) {
    this.setState({ password: value });
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
      message: '',
    });
  }

  get iForgot(): Element<*> {
    return <a onClick={this.onForgotClick} className={s.forgotLink}>i forgot</a>;
  }

  get infoMessage(): ?Element<*> {
    const { message } = this.state;
    if (!message) return null;
    return <Alert className={s.alert} type={Alert.SUCCESS}>{message}</Alert>;
  }

  get errorMessage(): ?Element<*> {
    const err = this.props.authenticationState.err;
    if (!err) return null;
    return <ApiErrors response={err} />;
  }

  get content(): Element<*> {
    const { org, email, password } = this.state;

    return (
      <div className={s.content}>
        {this.infoMessage}
        <SocialButton type="google" onClick={this.onGoogleSignIn} fullWidth>
          Sign In with Google
        </SocialButton>
        <Form className={s.form} onSubmit={this.submitLogin}>
          <WrapToLines className={s.orLine}>or</WrapToLines>
          {this.errorMessage}
          <FormField label="Organization" required>
            <TextInput onChange={this.onOrgChange} value={org} type="text" className="fc-input" autoFocus />
          </FormField>
          <FormField label="Email" required>
            <TextInput onChange={this.onEmailChange} value={email} type="text" className="fc-input" />
          </FormField>
          <FormField label="Password" labelAtRight={this.iForgot} required>
            <TextInput onChange={this.onPasswordChange} value={password} type="password" className="fc-input" />
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
    authenticationState: _.get(state.asyncActions, 'authenticate', {}),
  };
}

export default connect(mapStateToProps, userActions)(Login);
