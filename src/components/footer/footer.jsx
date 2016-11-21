/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';

import styles from './footer.css';

import Icon from 'ui/icon';
import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';
import { Link } from 'react-router';

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
          <li><Link to="/stores">STORES</Link></li>
          <li><Link to="/gift-cards">GIFT CARDS</Link></li>
          <li><Link to="/frequently-asked-questions">FAQ</Link></li>
          <li><Link to="/shipping-and-returns">SHIPPING & RETURNS</Link></li>
        </ul>

        <div styleName="social-links">
          <Link to="https://www.instagram.com/theperfectgourmet/" target="_blank">
            <Icon name="fc-instagram" styleName="social-icon"/>
          </Link>
          <Link to="https://www.facebook.com/PerfectGourmet/" target="_blank">
            <Icon name="fc-facebook" styleName="social-icon"/>
          </Link>
          <Link to="https://twitter.com/perfectgourmet1" target="_blank">
            <Icon name="fc-twitter" styleName="social-icon"/>
          </Link>
          <Link to="https://www.pinterest.com/perfectgourmet/" target="_blank">
            <Icon name="fc-pinterest" styleName="social-icon"/>
          </Link>
        </div>
      </div>

      <div styleName="copyright">
        <p>&copy; COPYRIGHT 2016. THE PERFECT GOURMET</p>
        <ul>
          <li><Link to="terms-of-use">Terms</Link></li>
          <li><Link to="privacy-policy">Privacy</Link></li>
        </ul>
        <p><a href="http://foxcommerce.com/" target="_blank">POWERED BY FOXCOMMERCE</a>.</p>
      </div>
    </section>
  );
};

export default Footer;
