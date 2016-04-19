/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { toggleSidebar } from 'modules/sidebar';
import { toggleActive, resetTerm } from 'modules/search';

import styles from './storefront.css';

import Icon from 'ui/icon';
import Categories from '../categories/categories';
import Sidebar from '../sidebar/sidebar';
import Footer from '../footer/footer';
import Search from '../search/search';
import Cart from '../cart/cart';
import UserTools from '../usertools/usertools';


type StoreFrontProps = {
  children: HTMLElement;
  isSearchActive: boolean;
  toggleSidebar: Function;
  toggleSearch: Function;
  resetTerm: Function;
  location: any;
}

const StoreFront = (props : StoreFrontProps) : HTMLElement => {
  const changeCategoryCallback = () => {
    props.resetTerm();

    if (props.isSearchActive) {
      props.toggleSearch();
    }
  };

  return (
    <div styleName="container">
      <div styleName="content-container">
        <div styleName="storefront">
          <div styleName="head">
            <div styleName="search">
              <Search onSearch={props.toggleSearch}/>
            </div>
            <div styleName="hamburger" onClick={props.toggleSidebar}>
              <Icon name="fc-hamburger" styleName="head-icon"/>
            </div>
            <div styleName="logo-link">
              <Link to="/">
                <Icon styleName="logo" name="fc-some_brand_logo" />
              </Link>
            </div>
            <div styleName="tools">
              <UserTools path={props.location.pathname}/>
            </div>
          </div>
          <div styleName="categories">
            <Categories onClick={changeCategoryCallback} />
          </div>
          {props.children}
        </div>
      </div>
      <div styleName="footer">
        <Footer />
      </div>
      <div styleName="mobile-sidebar">
        <Sidebar path={props.location.pathname} />
      </div>
      <div>
        <Cart />
      </div>
    </div>
  );
};

const mapState = state => ({
  isSearchActive: state.search.isActive,
});

export default connect(mapState, {
  toggleSidebar,
  toggleSearch: toggleActive,
  resetTerm,
})(StoreFront);
