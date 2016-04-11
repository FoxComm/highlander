/* @flow */

import _ from 'lodash';
import React from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import { toggleSidebar } from 'modules/sidebar';
import { toggleActive, resetTerm } from 'modules/search';
import { toggleCart } from 'modules/cart';
import L from 'lib/i18n/message';

import styles from './storefront.css';

import Icon from 'ui/icon';
import { Link } from 'react-router';
import Categories from '../categories/categories';
import Sidebar from '../sidebar/sidebar';
import Footer from '../footer/footer';
import Search from '../search/search';
import Cart from '../cart/cart';


type StoreFrontProps = {
  children: HTMLElement;
  isSearchActive: boolean;
  toggleSidebar: Function;
  toggleSearch: Function;
  resetTerm: Function;
  toggleCart: Function;
}

const StoreFront = (props : StoreFrontProps) : HTMLElement => {
  const changeCategoryCallback = () => {
    props.resetTerm();

    if (props.isSearchActive) {
      props.toggleSearch();
    }
  };

  const user = _.get(props, ['auth', 'user'], null);
  const sessionLink = _.isEmpty(user) ?
    <Link to="/login" styleName="login-link"><L text="LOG IN" /></Link> :
    `HI, ${user.name.toUpperCase()}`;

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
            <Icon styleName="logo" name="fc-some_brand_logo" />
            <div styleName="tools">
              <div styleName="login">
                {sessionLink}
              </div>
              <div styleName="cart" onClick={props.toggleCart}>
                <Icon name="fc-cart" styleName="head-icon"/>
              </div>
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
        <Sidebar />
      </div>
      <div>
        <Cart />
      </div>
    </div>
  );
};

const mapState = state => ({
  auth: state.auth,
  isSearchActive: state.search.isActive,
});

export default connect(mapState, { toggleSidebar, toggleCart, toggleSearch: toggleActive, resetTerm })(StoreFront);
