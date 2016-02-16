
import React from 'react';
import cssModules from 'react-css-modules';
import styles from './css/login.css';

import { TextInput } from '../common/inputs';
import { Form, FormField } from '../forms';

@cssModules(styles)
export default class Login extends React.Component {

  state = {
    email: '',
    password: '',
  };

  onChangeEmail({target}) {
    this.setState({
      email: target.value,
    });
  }

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
        <div>------------- or ------------</div>
        <Form>
          <FormField>
            <TextInput placeholder="EMAIL" value={email} onChane={::this.onChangeEmail} />
          </FormField>
          <FormField>
            <TextInput placeholder="PASSWORD" value={password} onChane={::this.onChangePassword} type="password" />
          </FormField>
        </Form>
      </div>
    );
  }
}
