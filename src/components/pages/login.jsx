
import React from 'react';
import cssModules from 'react-css-modules';
import styles from './css/login.css';
import { autobind } from 'core-decorators';

import { TextInput } from '../common/inputs';
import { Form, FormField } from '../forms';
import Button from '../common/buttons';
import WrapToLines from '../common/wrap-to-lines';


@cssModules(styles)
export default class Login extends React.Component {

  state = {
    email: '',
    password: '',
  };

  @autobind
  onChangeEmail({target}) {
    this.setState({
      email: target.value,
    });
  }

  @autobind
  onChangePassword({target}) {
    this.setState({
      password: target.value,
    });
  }

  render() {
    const { email, password } = this.state;

    return (
      <div styleName="login-block">
        <div styleName="title">LOG IN</div>
        <Button icon="fc-google" styleName="google-login">LOG IN WITH GOOGLE</Button>
        <WrapToLines>or</WrapToLines>
        <Form>
          <FormField styleName="form-field">
            <TextInput placeholder="EMAIL" value={email} onChange={this.onChangeEmail} />
          </FormField>
          <FormField styleName="form-field">
            <TextInput placeholder="PASSWORD" value={password} onChange={this.onChangePassword} type="password" />
          </FormField>
          <Button>LOG IN</Button>
        </Form>
      </div>
    );
  }
}
