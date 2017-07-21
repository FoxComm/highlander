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
        <Link styleName="link title">
          Customer Service
        </Link>

        <Link styleName="link">
          Shipping
        </Link>
        <Link styleName="link">
          Returns
        </Link>
        <Link styleName="link">
          Payment Methods
        </Link>
        <Link styleName="link">
          Service & Repairs
        </Link>
        <Link styleName="link">
          Replacement Parts
        </Link>
        <Link styleName="link">
          Warranty
        </Link>
        <Link styleName="link">
          Airline Carry-On Guide
        </Link>
        <Link styleName="link">
          FAQs
        </Link>
        <Link styleName="link">
          Gift Card & Services
        </Link>
        <Link styleName="link">
          Setting Your TUMI Lock
        </Link>
        <Link styleName="link">
          TUMI Global Locator
        </Link>
      </div>
    );
  }

  get contactUs() {
    return (
      <div styleName="link-column">
        <Link styleName="link title">
          Contact Us
        </Link>

        <Link styleName="link">
          1-800-299-8864
        </Link>

        <Link styleName="link title">
          My Account
        </Link>

        <Link styleName="link">
          Sign In
        </Link>
        <Link styleName="link">
          Track Orders
        </Link>
        <Link styleName="link">
          Register Your TUMI
        </Link>
      </div>
    );
  }

  get aboutTumi() {
    return (
      <div styleName="link-column">
        <div styleName="link title">
          About Tumi
        </div>

        <Link styleName="link">
          TUMI Difference
        </Link>
        <Link styleName="link">
          Corporate Responsibility
        </Link>
        <Link styleName="link">
          California Transparency in Supply Chain Act
        </Link>
        <Link styleName="link">
          Web Accessibility Statement
        </Link>
        <Link styleName="link">
          Careers
        </Link>

        <Link styleName="link title">
          CORPORATE CONTACTS
        </Link>

        <Link styleName="link">
          Global Press
        </Link>
        <Link styleName="link">
          Corporate Gifts & Incentives
        </Link>
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
          <div styleName="info-block">

            <div styleName="block links">
              {this.customerServiceBlock}
              {this.contactUs}
              {this.aboutTumi}
            </div>

            <div styleName="block links links-mobile">
              {this.mobileLinks}
            </div>

            <div styleName="block store">
              <div styleName="title">My Store</div>
              <img styleName="store-image" src="https://tumi.scene7.com/is/image/Tumi/0001000748?wid=1200" />

              <div styleName="store-name">Tumi Store - Novinski Passage</div>
              <div styleName="store-address">
                Novinski Boulevard 31<br />
                Moscow, 123242
              </div>
              <a styleName="store-phone" href="#">74957874114 505</a>
            </div>

            <div styleName="block contact-us-mobile">
              {this.mobileContactUs}
            </div>

            <div styleName="block socials">
              <div styleName="title">Follow Us</div>

              <div styleName="icons">
                {Object.keys(followUs).map((sn: string) => (
                  <a href={followUs[sn]} key={sn}>
                    <Icon styleName="socials-icon" name={`fc-${sn}`} />
                  </a>
                ))}
              </div>
            </div>

            <div styleName="block newsletter">
              <div styleName="title">Sign up for newsletter</div>
              <TextInput
                blockClassName={styles.input}
                value={this.state.email}
                onChange={this.handleChangeEmail}
                placeholder="Enter your email address"
              />
            </div>

            <div styleName="block tracer">
              <div styleName="title">Register your TUMI</div>
              <TextInput
                blockClassName={styles.input}
                value={this.state.tracer}
                onChange={this.handleChangeTracer}
                placeholder="Enter your 20-digit tracer #"
              />
            </div>
          </div>

          <div styleName="copyrights-block">
            <Link to="/" styleName="link">
              <Logo styleName="logo" />
            </Link>
            <div styleName="copyrights">
              ©{new Date().getFullYear()}&nbsp;Tumi,&nbsp;Inc.
            </div>
          </div>

        </div>
      </div>
    );
  }
}

export default Footer;
