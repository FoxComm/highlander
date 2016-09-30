/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';

import styles from './footer.css';

import Icon from 'ui/icon';
import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';

const Footer = () : HTMLElement => {
  return (
    <section styleName="footer">
      <header styleName="heading">
        <h2 styleName="title">JOIN OUR NEWSLETTER</h2>
        <h3 styleName="subtitle">GET ACCESS TO SPECIAL PROMOTIONS AND OUR NEWEST CREATIONS</h3>
      </header>

      <div styleName="wrap">
        <form styleName="email">
          <TextInput placeholder="Email" />
          <Button styleName="button" type="button">Join</Button>
        </form>

        <ul styleName="links">
          <li><a href="#">ABOUT US</a></li>
          <li><a href="#">STORES</a></li>
          <li><a href="#">GIFT CARDS</a></li>
          <li><a href="#">FAQ</a></li>
          <li><a href="#">SHIPPING & RETURNS</a></li>
        </ul>

        <div styleName="social-links">
          <Icon name="fc-instagram" styleName="social-icon"/>
          <Icon name="fc-facebook" styleName="social-icon"/>
          <Icon name="fc-twitter" styleName="social-icon"/>
          <Icon name="fc-pinterest" styleName="social-icon"/>
        </div>
      </div>

      <div styleName="copyright">
        <p>&copy; COPYRIGHT 2016. THE PERFECT GOURMET</p>
        <ul>
          <li><a href="">Terms</a></li>
          <li><a href="">Privacy</a></li>
        </ul>
        <p>POWERED BY FOXCOMMERCE.</p>
      </div>
    </section>
  );
};

export default Footer;
