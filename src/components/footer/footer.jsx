/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';

import styles from './footer.css';

import Icon from 'ui/icon';
import { TextInputWithLabel } from 'ui/inputs';

const Footer = () : HTMLElement => {
  return (
    <div styleName="footer">
      <div styleName="wrap">
        <div styleName="links">
          <ul>
            <h3>Company</h3>
            <li><a href="#">About us</a></li>
            <li><a href="#">Jobs</a></li>
            <li><a href="#">Gift Cards</a></li>
          </ul>
          <ul>
            <h3>Help</h3>
            <li><a href="#">FAQ</a></li>
            <li><a href="#">Shipping</a></li>
            <li><a href="#">Returns</a></li>
          </ul>
          <ul>
            <h3>Stores</h3>
            <li><a href="#">New York, NY</a></li>
            <li><a href="#">Los Angeles, LA</a></li>
            <li><a href="#">Las Vegas, NV</a></li>
          </ul>
        </div>

        <div styleName="social">
          <div styleName="social-links">
            <Icon name="fc-instagram" styleName="social-icon" />
            <Icon name="fc-facebook" styleName="social-icon" />
            <Icon name="fc-twitter" styleName="social-icon" />
            <Icon name="fc-pinterest" styleName="social-icon" />
          </div>
          <TextInputWithLabel placeholder="Stay in to know" label={<Icon name="fc-arrow" />}/>
        </div>
      </div>

      <div styleName="copyright">
        <p>&copy; Some Brand. All Rights Reserved</p>
        <ul>
          <li><a href="">Terms of Use</a></li>
          <li><a href="">Privacy Policy</a></li>
        </ul>
      </div>
    </div>
  );
};

export default Footer;
