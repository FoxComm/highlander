/* @flow */

import React, { Element } from 'react';
import { Link } from 'react-router';
import Icon from 'ui/icon';

import styles from './footer.css';

const Footer = () : Element<*> => {
  return (
    <div styleName="footer-wrap">
      <div styleName="footer">
        <div styleName="wrap">
          <Link to="/" styleName="link">
            <Icon styleName="logo-icon" name="fc-logo" />
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Footer;
