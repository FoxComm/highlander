
import React from 'react';
import cssModules from 'react-css-modules';
import styles from './css/login.css';

import { TextInput } from '../common/inputs';

@cssModules(styles)
export default class Login extends React.Component {

  state = {
    email: '',
    password: '',
  };

  onChangeEmail({target}) {
    this.setState({
      email: target.value
    });
  }

  onChangePassword({target}) {
    this.setState({
      password: target.value
    });
  }

  render() {
    const { email, password } = this.state;

    return (
      <div styleName="block">
        <div styleName="title">LOG IN</div>
        <div>------------- or ------------</div>
        <form>
          <TextInput placeholder="EMAIL" value={email} onChane={::this.onChangeEmail} />
          <TextInput placeholder="PASSWORD" value={password} onChane={::this.onChangePassword} type="password" />
        </form>
      </div>
    );
  }
}
