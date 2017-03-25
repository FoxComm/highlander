/* @flow */

import React, { Element } from 'react';
import { Link } from 'react-router';
import Icon from 'ui/icon';

import styles from './footer.css';

const Footer = () : Element<*> => {
  return (
    <section styleName="footer">
      <div styleName="wrap">
        <Link to="/" styleName="link">
          <Icon styleName="logo-icon" name="fc-logo" />
        </Link>
      </div>
    </section>
  );
};

export default Footer;
