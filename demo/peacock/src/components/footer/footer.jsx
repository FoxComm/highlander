/* @flow */

import React, { Element } from 'react';

import styles from './footer.css';

const Footer = () : Element<*> => {
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
