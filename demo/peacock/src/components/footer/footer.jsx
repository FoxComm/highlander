/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';

import styles from './footer.css';

import Icon from 'ui/icon';
import { Link } from 'react-router';
import SubscriptionForm from '../email-subscription/form';
import Copyright from './copyright';

const Footer = () : HTMLElement => {
  return (
    <section styleName="footer">
      <div styleName="wrap">
        <div styleName="logo">
          PURE
        </div>
      </div>
    </section>
  );
};

export default Footer;
