/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import cssModules from 'react-css-modules';
import styles from './storefront.css';
import { connect } from 'react-redux';
import { toggleSidebar } from 'modules/sidebar';

import Icon from 'ui/icon';
import { Link } from 'react-router';
import Categories from '../categories/categories';
import Sidebar from '../sidebar/sidebar';
import Footer from '../footer/footer';


type StoreFrontProps = {
  children: HTMLElement;
  toggleSidebar: Function;
}

const StoreFront = (props : StoreFrontProps) : HTMLElement => {
  return (
    <div styleName="container">
      <div styleName="content-container">
        <div styleName="storefront">
          <div styleName="head">
            <div styleName="search">
              <Icon name="fc-magnifying-glass" styleName="head-icon"/>
            </div>
            <div styleName="hamburger" onClick={props.toggleSidebar}>
              <Icon name="fc-hamburger" styleName="head-icon"/>
            </div>
            <Icon styleName="logo" name="fc-some_brand_logo" />
            <div styleName="tools">
              <div styleName="login">
                <Link to="/login" styleName="login-link">LOG IN</Link>
              </div>
              <div styleName="cart">
                <Icon name="fc-cart" styleName="head-icon"/>
              </div>
            </div>
          </div>
          <div styleName="categories">
            <Categories />
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
    </div>
  );
};

export default connect(null, {toggleSidebar})(cssModules(StoreFront, styles));
