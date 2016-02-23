/* @flow */

import React, { PropTypes, Component, Element } from 'react';
import cssModules from 'react-css-modules';
import styles from './css/auth.css';
import { autobind } from 'core-decorators';
import { routeActions } from 'react-router-redux';
import { connect } from 'react-redux';

import { TextInput } from '../common/inputs';
import { Form, FormField } from '../forms';
import Button from '../common/buttons';
import WrapToLines from '../common/wrap-to-lines';
import Icon from '../common/icon';
import Link from '../common/link';

type AuthState = {
  email: string,
  password: string,
  username: string
};

/* ::`*/
@connect(state => state.auth)
@cssModules(styles)
/* ::`*/
export default class Auth extends Component {

  static propTypes = {
    stage: PropTypes.oneOf(['login', 'signup']),
    dispatch: PropTypes.func,
  };

  state: AuthState = {
    email: '',
    password: '',
    username: '',
  };

  @autobind
  onChangeEmail({target}: SEvent<HTMLInputElement>) {
    this.setState({
      email: target.value,
    });
  }

  @autobind
  onChangePassword({target}: SEvent<HTMLInputElement>) {
    this.setState({
      password: target.value,
    });
  }

  @autobind
  onChangeUsername({target}: SEvent<HTMLInputElement>) {
    this.setState({
      username: target.value,
    });
  }

  @autobind
  onSwitchStage(event: SEvent<HTMLInputElement>) {
    event.preventDefault();
    this.props.dispatch(routeActions.push(this.props.stage == 'login' ? '/signup' : '/login'));
  }

  get signWithGoogleText(): string {
    return this.props.stage == 'login' ? 'LOG IN WITH GOOGLE' : 'SIGN UP WITH GOOGLE';
  }

  get formFields(): Array<Element> {
    const { email, password, username } = this.state;
    const { stage } = this.props;

    const passwordPlaceholder = stage == 'login' ? 'PASSWORD' : 'CREATE PASSWORD';

    let fields = [
      <FormField key="email" styleName="form-field">
        <TextInput placeholder="EMAIL" value={email} type="email" onChange={this.onChangeEmail} />
      </FormField>,
      <FormField key="passwd" styleName="form-field">
        <TextInput placeholder={passwordPlaceholder}
          value={password} onChange={this.onChangePassword} type="password"
        />
      </FormField>,
    ];

    if (stage == 'signup') {
      fields = [
        <FormField key="username" styleName="form-field">
          <TextInput placeholder="FIRST & LAST NAME" value={username} onChange={this.onChangeUsername} />
        </FormField>,
        ...fields,
      ];
    }

    return fields;
  }

  get switchStage(): Element {
    if (this.props.stage == 'login') {
      return (
        <span>
          Don’t have an account? <Link href="/signup" onClick={this.onSwitchStage}>Sign Up</Link>
        </span>
      );
    }

    return (
      <span>
        Already have an account? <Link href="/login" onClick={this.onSwitchStage}>Log in</Link>
      </span>
    );
  }

  get title(): string {
    return this.props.stage == 'login' ? 'LOG IN' : 'SIGN UP';
  }

  render(): Element {
    return (
      <div styleName="login-block">
        <Icon styleName="icon" name="fc-some_brand_logo" />
        <div styleName="title">{this.title}</div>
        <Button icon="fc-google" styleName="google-login">{this.signWithGoogleText}</Button>
        <WrapToLines styleName="divider">or</WrapToLines>
        <Form>
          {this.formFields}
          <Button styleName="login-button">{this.title}</Button>
        </Form>
        <div styleName="switch-stage">
          {this.switchStage}
        </div>
      </div>
    );
  }
}
