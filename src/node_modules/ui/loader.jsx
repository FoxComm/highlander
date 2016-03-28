import React from 'react';
import Icon from 'ui/icon';
import type { HTMLElement } from 'types';
import styles from './css/loader.css';

const Loader = (): HTMLElement => {
  return (
    <div styleName="loader">
      <Icon name="fc-ripple" size="xl"/>
    </div>
  );
};

export default Loader;
