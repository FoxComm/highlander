/* @flow */

import React, { Element } from 'react';
import _ from 'lodash';

import styles from './banner.css';

type PartProps = {
  iconClassName: string,
  title: string,
  subtitle?: string|Element,
};

const contactUs = () => {
  return (
    <p styleName="marginless">
      1-855-528-8495<br />
      Monday to Friday<br />
      9:30am - 6pm EST
    </p>
  );
};

const items = [
  {
    iconClassName: styles.delivery,
    title: 'FREE GROUND SHIPPING',
    subtitle: 'On All Orders',
  },
  {
    iconClassName: styles.returns,
    title: 'FREE RETURNS',
  },
  {
    iconClassName: styles.samples,
    title: '2 FREE SAMPLES',
    subtitle: 'Per Order',
  },
  {
    iconClassName: styles.gifts,
    title: 'LUXURY GIFT BOX',
  },
  {
    iconClassName: styles.contact,
    title: 'CONTACT US',
    subtitle: contactUs(),
  },
];

const BannerPart = (props: PartProps) => {
  return (
    <div styleName="banner-item">
      <div className={`${styles.icon} ${props.iconClassName}`} />
      <div styleName="banner-title">
        {props.title}
      </div>
      <div styleName="banner-subtitle">
        {props.subtitle}
      </div>
    </div>
  );
};

const Banner = () => {
  return (
    <div styleName="banner">
      <div styleName="parts">
        <div styleName="title">
          SHOP THE BOUDOIR ONLINE WITH BENEFITS
        </div>
        <div styleName="banners">
          {_.map(items, item => (<BannerPart {...item} />))}
        </div>
      </div>
    </div>
  );
};

export default Banner;
