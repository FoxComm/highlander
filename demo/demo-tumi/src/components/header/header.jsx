/* @flow */

import React from 'react';
import { connect } from 'react-redux';
import classnames from 'classnames';

// actions
import { toggleSidebar, toggleSearch } from 'modules/ui';

// components
import { Link } from 'react-router';
import ActionLink from 'ui/action-link/action-link';
import UserTools from 'components/usertools/usertools';
import Navigation from 'components/navigation/navigation';
import Sidebar from 'components/sidebar/sidebar';
import Logo from 'components/logo/logo';
import SearchIcon from './search-icon';
import Search from 'components/search/search';

import styles from './header.css';

type Props = {
  toggleSidebar: () => any,
  toggleSearch: (shouldShow: boolean) => void,
  path: string,
  query: ?Object,
  searchVisible: boolean,
  sidebarVisible: boolean,
};

const Header = (props: Props) => {
  const menuIcon = {
    name: !props.sidebarVisible ? 'fc-hamburger' : 'fc-close',
    size: 'm',
  };

  const searchBarClass = classnames(styles.searchbar, {
    _open: props.searchVisible,
  });

  const handleClickOutside = (e: SyntheticEvent) => {
    if (props.searchVisible) {
      e.stopPropagation();
      props.toggleSearch(false);
    }
  };

  return (
    <div styleName="header-wrap">
      <div id="header" styleName="header">
        <div styleName="wrap">
          <ActionLink
            action={props.toggleSidebar}
            icon={menuIcon}
            styleName="action-link-menu"
          />
          <Link to="/" styleName="logo-link-mobile">
            <Logo styleName="logo" />
          </Link>
          <div styleName="tools">
            <SearchIcon onClick={() => props.toggleSearch()} />
            <UserTools path={props.path} query={props.query} />
          </div>
        </div>
      </div>

      <div styleName="nav-search-logo-wrapper">
        <Link to="/" styleName="logo-link">
          <Logo styleName="logo" />
        </Link>
        <div styleName="navigation">
          <Navigation
            path={props.path}
            onToggleSearch={props.toggleSearch}
            isSearchExpanded={props.searchVisible}
          />
        </div>
      </div>

      <div styleName="mobile-sidebar">
        <Sidebar path={props.path} />
      </div>

      <div className={searchBarClass}>
        <Search
          visible={props.searchVisible}
          onClickOutside={handleClickOutside}
          onClose={() => props.toggleSearch(false)}
        />
      </div>
    </div>
  );
};

const mapState = state => ({
  sidebarVisible: state.ui.sidebarVisible,
  searchVisible: state.ui.searchVisible,
});

export default connect(mapState, {
  toggleSidebar, toggleSearch,
})(Header);
