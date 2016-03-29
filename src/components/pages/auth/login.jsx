import classNames from 'classnames';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';
import { connect } from 'react-redux';

import styles from './auth.css';

import { TextInput, TextInputWithLabel } from 'ui/inputs';
import { FormField } from 'ui/forms';
import Button from 'ui/buttons';
import WrapToLines from 'ui/wrap-to-lines';
import { Link } from 'react-router';

import * as actions from 'modules/auth';

import type { HTMLElement } from 'types';


type AuthState = {
  email: string,
  password: string,
};

const mapState = state => ({
  isLoading: state.auth.isFetching,
});

/* ::`*/
@connect(mapState, actions)
/* ::`*/
export default class Auth extends Component {

  state: AuthState = {
    email: '',
    password: '',
  };

  @autobind
  onChangeEmail({target}: any) {
    this.setState({
      email: target.value,
    });
  }

  @autobind
  onChangePassword({target}: any) {
    this.setState({
      password: target.value,
    });
  }

  @autobind
  authenticate(e: any) {
    e.preventDefault();
    e.stopPropagation();
    const { email, password } = this.state;
    const kind = 'customer';
    this.props.authenticate({email, password, kind}).then(() => {
      browserHistory.push('/');
    }).catch(err => {
      console.error(err);
    });
  }

  render(): HTMLElement {
    const { password, email } = this.state;

    const clsBtnLoading = classNames({
      'primary-button': !this.props.isLoading,
      'primary-button-loading': this.props.isLoading,
    });

    return (
      <div>
        <div styleName="title">LOG IN</div>
        <form>
          <Button icon="fc-google" onClick={this.props.googleSignin} type="button" styleName="google-login">
            LOG IN WITH GOOGLE
          </Button>
        </form>
        <WrapToLines styleName="divider">or</WrapToLines>
        <form>
          <FormField key="email" styleName="form-field">
            <TextInput placeholder="EMAIL" value={email} type="email" onChange={this.onChangeEmail} />
          </FormField>
          <FormField key="passwd" styleName="form-field">
            <TextInputWithLabel placeholder="PASSWORD"
              label={!password && <Link styleName="restore-link" to="/password/restore">forgot ?</Link>}
              value={password} onChange={this.onChangePassword} type="password"
            />
          </FormField>
          <Button styleName={clsBtnLoading} onClick={this.authenticate}>LOG IN</Button>
        </form>
        <div styleName="switch-stage">
          Don’t have an account? <Link styleName="signup-link" to="/signup">Sign Up</Link>
        </div>
      </div>
    );
  }
}
