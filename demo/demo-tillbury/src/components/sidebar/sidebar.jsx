/* @flow */

import React from 'react';

// libs
import omit from 'lodash/omit';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import { isAuthorizedUser } from 'paragons/auth';

// actions
import { logout } from 'modules/auth';
import { fetch as fetchCart } from 'modules/cart';
import * as actions from 'modules/sidebar';

// components
import SidebarNavigation from 'components/navigation/sidebar-navigation';
import Overlay from 'ui/overlay/overlay';

import styles from './sidebar.css';

type SidebarProps = Localized & {
  isVisible: boolean,
  toggleSidebar: Function,
  path: string,
};

type State = {
  searchFocused: boolean,
  backButton: () => ?Element,
};

class Sidebar extends React.Component {
  props: SidebarProps;

  state: State = {
    searchFocused: false,
    backButton: () => null,
  };

  @autobind
  setFocus(focus: boolean) {
    this.setState({ searchFocused: focus });
  }

  @autobind
  handleLogout(e) {
    e.preventDefault();
    this.props.logout().then(() => {
      this.props.fetchCart();
    });
  }

  @autobind
  onLinkClick(e) {
    if (e.target.tagName.toLowerCase() === 'a') {
      this.props.toggleSidebar();
    }
  }

  get userAuthorized() {
    return isAuthorizedUser(this.props.user);
  }

  get renderSessionLink() {
    const { t } = this.props;

    if (this.userAuthorized) {
      return (
        <a styleName="controls-session-link" onClick={this.handleLogout}>
          {this.props.t('Log out')}
        </a>
      );
    }

    return (
      <Link
        styleName="controls-session-link"
        to={{ pathname: this.props.path, query: { auth: 'LOGIN' } }}
        children={t('Sign in')}
      />
    );
  }

  get myProfileLink() {
    if (this.userAuthorized) {
      return (
        <Link
          to="/profile"
          styleName="session-link"
          activeClassName={styles['active-link']}
          children="Profile"
        />
      );
    }
  }

  get controls() {
    const backButton = this.state.backButton();

    if (!backButton) return null;

    return (
      <div styleName="controls-close">
        {backButton}
      </div>
    );
  }

  @autobind
  changeBackButton(backFabric: Function) {
    this.setState({ backButton: backFabric });
  }

  render() {
    const { isVisible, toggleSidebar, path, t } = this.props;

    const sidebarClass = classNames({
      'sidebar-hidden': !isVisible,
      'sidebar-shown': isVisible,
    });

    return (
      <div styleName={sidebarClass}>
        <Overlay onClick={toggleSidebar} shown={isVisible} />
        <div styleName="container">
          <div styleName="controls">
            {this.controls}
          </div>
          <div styleName="links-group" onClick={this.onLinkClick}>
            <div styleName="controls-categories">
              <SidebarNavigation
                path={path}
                renderBack={this.changeBackButton}
              />
            </div>
            <div styleName="controls-session-wrapper">
              {this.myProfileLink}
              {this.renderSessionLink}
              <a styleName="controls-session-link" href="#/stores">{t('Stores')}</a>
              <a styleName="controls-session-link" href="#/customer-service">{t('Customer service')}</a>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

const mapStates = state => ({
  ...state.sidebar,
  ...omit(state.auth, 'isVisible'),
});

export default connect(mapStates, {
  ...actions,
  logout,
  fetchCart,
})(localized(Sidebar));
