/* @flow */

import React from 'react';

// libs
import { connect } from 'react-redux';
import { Link } from 'react-router';
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import { isAuthorizedUser } from 'paragons/auth';

//actions
import { logout } from 'modules/auth';
import { fetch as fetchCart } from 'modules/cart';
import * as actions from 'modules/sidebar';

// components
import Categories from 'components/navigation/navigation';
import Search from 'components/search/search';
import Overlay from 'ui/overlay/overlay';
import ActionLink from 'ui/action-link/action-link';

import styles from './sidebar.css';

type SidebarProps = Localized & {
  isVisible: boolean,
  toggleSidebar: Function,
  path: string,
};

type State = {
  searchFocused: boolean,
}


class Sidebar extends React.Component {
  props: SidebarProps;

  state: State = {
    searchFocused: false,
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
    if (e.target.tagName === 'A') {
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
        <a styleName="session-link" onClick={this.handleLogout}>
          {this.props.t('Log out')}
        </a>
      );
    }

    return (
      <Link
        styleName="session-link"
        to={{pathname: this.props.path, query: {auth: 'LOGIN'}}}
        children={t('Log in')}
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

  render() {
    const sidebarClass = classNames({
      'sidebar-hidden': !this.props.isVisible,
      'sidebar-shown': this.props.isVisible,
    });

    return (
      <div styleName={sidebarClass}>
        <Overlay onClick={this.props.toggleSidebar} shown={this.props.isVisible} />
        <div styleName="container">
          <div styleName="controls">
            <div styleName="controls-close">
              <ActionLink
                action={this.props.toggleSidebar}
                title="Close"
                styleName="action-link-close"
              />
            </div>
            <div styleName={this.state.searchFocused ? 'controls-search-focused' : 'controls-search'}>
              <Search onSearch={this.props.toggleSidebar} setFocus={this.setFocus} isActive />
            </div>
            <div styleName="links-group" onClick={this.onLinkClick}>
              <div styleName="controls-categories">
                <Categories
                  path={this.props.path}
                />
              </div>
              <div styleName="controls-session-wrapper">
                <div styleName="controls-session">
                  {this.myProfileLink}
                </div>
                <div styleName="controls-session">
                  {this.renderSessionLink}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

const mapStates = state => ({
  ...state.sidebar,
  ...state.auth,
});

export default connect(mapStates, {
  ...actions,
  logout,
  fetchCart,
})(localized(Sidebar));
