/* @flow */

import React, { PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './storefront.css';

import Icon from '../common/icon';
import { Link } from 'react-router';

const StoreFront = props => {
  return (
    <div styleName="storefront">
      <div styleName="head">
        <div styleName="search">
          <Icon name="fc-magnifying-glass" />
        </div>
        <Icon styleName="logo" name="fc-some_brand_logo" />
        <div styleName="tools">
          <Link to="/login">LOG IN</Link>
          <Icon name="fc-cart" />
        </div>
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
