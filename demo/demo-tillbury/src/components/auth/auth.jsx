/* @flow  */

import classNames from 'classnames';
import React, { Component, Element } from 'react';
import { authBlockTypes } from 'paragons/auth';
import { assoc, dissoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// localization
import localized from 'lib/i18n';

import Overlay from 'ui/overlay/overlay';
import Login from './login';
import Signup from './signup';
import ResetPassword from './reset-password';
import RestorePassword from './restore-password';
import ForceRestorePassword from './force-restore-password';

// styles
import styles from './auth.css';

type Props = {
  authBlockType: string,
  path: Object,
  isVisible?: boolean,
  toggleMenu: () => void,
  t: any,
};

class Auth extends Component {
  props: Props;

  get body() {
    const authProps = {
      getPath: this.getPath,
      path: this.props.path,
      mergeGuestCart: true,
    };

    switch (this.props.authBlockType) {
      case authBlockTypes.LOGIN:
        return <Login {...authProps} />;
      case authBlockTypes.SIGNUP:
        return <Signup {...authProps} />;
      case authBlockTypes.RESET_PASSWORD:
        return <ResetPassword {...authProps} />;
      case authBlockTypes.FORCE_RESTORE_PASSWORD:
        return <ForceRestorePassword {...authProps} />;
      case authBlockTypes.RESTORE_PASSWORD:
        return <RestorePassword {...authProps} />;
      default:
        return <Login {...authProps} />;
    }
  }

  @autobind
  getPath(newType: ?string): Object {
    return newType ? assoc(this.props.path, ['query', 'auth'], newType) : dissoc(this.props.path, ['query', 'auth']);
  }

  render(): Element<*> {
    const { isVisible, toggleMenu, t } = this.props;

    const className = classNames({
      [styles.shown]: isVisible,
    });

    return (
      <div className={className}>
        <Overlay onClick={toggleMenu} shown={isVisible} />
        <div styleName="auth-block">
          <div styleName="my-account">
            {t('My Account')}
          </div>
          {this.body}
        </div>
      </div>
    );
  }
}

export default localized(Auth);
