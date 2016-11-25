/* @flow */

import React from 'react';

import Icon from 'ui/icon';

import styles from './top-banner.css';

const TopBanner = () => {
  return (
    <div styleName="banner">
      <div styleName="content">
        To celebrate the launch of our new site, use code
        <strong styleName="strong"> PERFECT20 </strong>
        and enjoy 20% off sitewide!
      </div>
      <div styleName="button">
        <a styleName="close" onClick={() => console.log('click!')}>
          <Icon name="fc-close" className="close-icon"/>
        </a>
      </div>
    </div>
  );
};

export default TopBanner;
