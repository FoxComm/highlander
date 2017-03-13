/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import classNames from 'classnames';
import { logout } from 'modules/auth';
import { fetch as fetchCart } from 'modules/cart';
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';
import { autobind } from 'core-decorators';

import { isAuthorizedUser } from 'paragons/auth';

import styles from './sidebar.css';

import Categories from '../navigation/navigation';
import Search from '../search/search';

import * as actions from 'modules/sidebar';

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
  }

  componentWillReceiveProps(nextProps) {
    let overflow, position;
    if(nextProps.isVisible) {
      overflow = 'hidden';
      position = 'absolute';
    }else {
      overflow = 'auto';
      position = 'relative';
    }
    Object.assign(document.body.style, {
      overflow,
      position,
    });
  }

  @autobind
  setFocus(focus) {
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
  };

  get userAuthorized() {
    return isAuthorizedUser(this.props.user);
  }

  get renderSessionLink() {
    return this.userAuthorized ?
        <a styleName="session-link" onClick={this.handleLogout}>
          {this.props.t('Log out')}
        </a>
      :
        <Link
          styleName="session-link"
          to={{pathname: this.props.path, query: {auth: 'LOGIN'}}}
        >
          {this.props.t('Log in')}
        </Link>
      ;
   }

   get myProfileLink() {
     return this.userAuthorized ?
        <Link
          to="/profile"
          styleName="session-link"
          activeClassName={styles['active-link']}
        >
          Profile
        </Link>
      :
        null
      ;
   }

  render(){
    const sidebarClass = classNames({
      'sidebar-hidden': !this.props.isVisible,
      'sidebar-shown': this.props.isVisible,
    });

    const { t } = this.props;

    const userAuthorized = this.userAuthorized;

    return (
      <div styleName={sidebarClass}>
        <div styleName="overlay" onClick={this.props.toggleSidebar}></div>
        <div styleName="container">
          <div styleName="controls">
            <div styleName="controls-close">
              <span styleName="close-button" onClick={this.props.toggleSidebar}>
                Close
              </span>
            </div>
            <div styleName={ this.state.searchFocused ? 'controls-search-focused' : 'controls-search'}>
              <Search onSearch={this.props.toggleSidebar} setFocus={this.setFocus} isActive/>
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
