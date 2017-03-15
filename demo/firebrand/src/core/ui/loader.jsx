import React from 'react';
import Icon from 'ui/icon';
import type { HTMLElement } from 'types';
import styles from './css/loader.css';

type Props = {
  size: 'm' | 'l' | 'xl' | 'xxl'
}

const Loader = (props: Props): HTMLElement => {
  return (
    <div styleName="loader" className={styles[`loader-${props.size}`]}>
      <Icon name="fc-ripple" size={props.size}/>
    </div>
  );
};

Loader.defaultProps = {
  size: 'xl',
};

export default Loader;
