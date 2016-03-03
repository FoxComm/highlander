/* @flow */

import React, { PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './storefront.css';

import Icon from '../common/icon';
import { Link } from 'react-router';
import Categories from '../categories/categories';

const StoreFront = props => {
  return (
    <div styleName="storefront">
      <div styleName="head">
        <div styleName="search">
          <Icon name="fc-magnifying-glass" styleName="head-icon"/>
        </div>
        <div styleName="hamburger">
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
      <div styleName="footer">
        Here will be footer
      </div>
    </div>
  );
};

StoreFront.propTypes = {
  children: PropTypes.node.isRequired,
};

export default cssModules(StoreFront, styles);
