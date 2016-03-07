/* @flow */

import React from 'react';
import styles from './footer.css';
import cssModules from 'react-css-modules';
import type { HTMLElement } from 'types';

import Icon from 'ui/icon';

const Footer = () : HTMLElement => {
  return (
    <div styleName="container">
      <div styleName="social-links">
        <Icon name="fc-instagram" styleName="social-icon" />
        <Icon name="fc-facebook" styleName="social-icon" />
        <Icon name="fc-twitter" styleName="social-icon" />
        <Icon name="fc-pinterest" styleName="social-icon" />
      </div>
      <div styleName="other-links">
        <a href="#" styleName="other-link">Terms of Use</a>
        <a href="#" styleName="other-link">Privacy Policy</a>
        <a href="#" styleName="other-link">Shipping &amp; Returns</a>
      </div>
    </div>
  );
};

export default cssModules(Footer, styles);
