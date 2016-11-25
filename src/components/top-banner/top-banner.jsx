/* @flow */

import React from 'react';
import classNames from 'classnames';

import Icon from 'ui/icon';

import styles from './top-banner.css';

type Props = {
  isVisible: boolean,
  onClose: Function,
};

const TopBanner = (props: Props) => {
  const bannerClass = classNames(styles.banner, {
    [styles._hidden]: !props.isVisible,
  });

  return (
    <div className={bannerClass}>
      <div styleName="content">
        To celebrate the launch of our new site, use code
        <strong styleName="strong"> PERFECT20 </strong>
        and enjoy 20% off sitewide!
      </div>
      <div styleName="button">
        <a styleName="close" onClick={props.onClose}>
          <Icon name="fc-close" className="close-icon"/>
        </a>
      </div>
    </div>
  );
};

export default TopBanner;
