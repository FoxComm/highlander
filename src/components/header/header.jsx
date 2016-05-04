import React, {PropTypes} from 'react';
import { connect } from 'react-redux';
import {autobind} from 'core-decorators';
import { Link } from 'react-router';

import { toggleSidebar } from 'modules/sidebar';
import { toggleActive, resetTerm } from 'modules/search';

import styles from './header.css';

import Icon from 'ui/icon';
import Search from '../search/search';
import UserTools from '../usertools/usertools';
import Categories from '../categories/categories';

type Props = {
  toggleSidebar: Function;
  toggleSearch: Function;
  isSearchActive: boolean;
  resetTerm: Function;
  path: string;
}

const Header = (props:Props) => {
  const changeCategoryCallback = () => {
    props.resetTerm();

    if (props.isSearchActive) {
      props.toggleSearch();
    }
  };

  return (
    <div styleName="header">
      <div styleName="hamburger" onClick={props.toggleSidebar}>
        <Icon name="fc-hamburger" styleName="head-icon"/>
      </div>
      <Link to="/" styleName="logo-link">
        <Icon styleName="logo" name="fc-some_brand_logo"/>
      </Link>
      <div styleName="categories">
        <Categories onClick={changeCategoryCallback} />
      </div>
      <div styleName="search">
        <Search onSearch={props.toggleSearch}/>
      </div>
      <div styleName="tools">
        <UserTools path={props.path}/>
      </div>
    </div>
  );
}

const mapState = state => ({
  isSearchActive: state.search.isActive,
});

export default connect(mapState, {
  toggleSidebar,
  toggleSearch: toggleActive,
  resetTerm,
})(Header);
