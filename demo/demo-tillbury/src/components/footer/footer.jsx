/* @flow */

import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { Link } from 'react-router';
import Logo from 'components/logo/logo';
import Icon from 'ui/icon';
import TextInput from 'ui/text-input/text-input';

import styles from './footer.css';

type State = {
  email: string,
  tracer: string,
};

const followUs = {
  facebook: 'http://www.facebook.com/TumiTravel',
  twitter: 'http://twitter.com/Tumitravel',
  pinterest: 'http://www.pinterest.com/tumitravel/',
  instagram: 'http://instagram.com/tumitravel',
};

class Footer extends Component {
  state: State = {
    email: '',
    tracer: '',
  };

  @autobind
  handleChangeEmail({ target }: any) {
    this.setState({ email: target.value });
  }

  @autobind
  handleChangeTracer({ target }: any) {
    this.setState({ tracer: target.value });
  }

  get customerServiceBlock() {
    return (
      <div styleName="link-column">
        <div styleName="link title">
          Customer Service
        </div>

        <Link styleName="link">
          Contact Us
        </Link>
        <Link styleName="link">
          Shipping Information
        </Link>
        <Link styleName="link">
          Returns Policy
        </Link>
        <Link styleName="link">
          Terms & Conditions
        </Link>
        <Link styleName="link">
          Privacy & Cookies
        </Link>
        <Link styleName="link">
          Store Locator
        </Link>
      </div>
    );
  }

  get contactUs() {
    return (
      <div styleName="link-column">
        <div styleName="link title">
          Customer Service
        </div>

        <Link styleName="link">
          FAQ
        </Link>
        <Link styleName="link">
          Popular Searches
        </Link>
      </div>
    );
  }

  get aboutTilbury() {
    return (
      <div styleName="link-column">
        <div styleName="link title">
          ABOUT CHARLOTTE
        </div>

        <Link styleName="link">
          Charlotte's Story
        </Link>
        <Link styleName="link">
          Charlotte's Work
        </Link>
        <Link styleName="link">
          Blog
        </Link>
        <Link styleName="link">
          Pro Artist Program
        </Link>
      </div>
    );
  }

  get followUs() {
    return (
      <div styleName="link-column">
        <div styleName="link title">
          Follow Us
        </div>

        <div styleName="social">
          <Link styleName="link social-link" style={{backgroundPosition: '-1px -174px'}} />
          <Link styleName="link social-link" style={{backgroundPosition: '-35px -174px'}} />
          <Link styleName="link social-link" style={{backgroundPosition: '-70px -174px'}} />
          <Link styleName="link social-link" style={{backgroundPosition: '-106px -174px'}} />
          <Link styleName="link social-link" style={{backgroundPosition: '-142px -174px', width: '30px'}} />
          <Link styleName="link social-link" style={{backgroundPosition: '-223px -174px'}} />
          <Link styleName="link social-link" style={{backgroundPosition: '-186px -174px', width: '33px'}} />
        </div>
      </div>
    );
  }

  get mobileLinks() {
    return (
      <div styleName="link-column">
        <Link styleName="link">
          Customer Service
        </Link>
        <Link styleName="link">
          Contact Us
        </Link>
        <Link styleName="link">
          My Account
        </Link>
        <Link styleName="link">
          About Tumi
        </Link>
        <Link styleName="link">
          Gift Card & Services
        </Link>
      </div>
    );
  }

  get mobileContactUs() {
    return (
      <div styleName="link-column">
        <Link styleName="link title">
          Contact Us
        </Link>
        <Link styleName="link">
          1-800-299-8864
        </Link>
      </div>
    );
  }

  render() {
    return (
      <div styleName="footer-wrap">
        <div styleName="footer">
          <div styleName="icon" />
          <div styleName="links">
            {this.customerServiceBlock}
            {this.contactUs}
            {this.aboutTilbury}
            {this.followUs}
          </div>
        </div>
      </div>
    );
  }
}

export default Footer;
