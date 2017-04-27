import React, { Element } from 'react';
import Icon from 'ui/icon';
import styles from './css/loader.css';

type Props = {
  size?: 'm' | 'l' | 'xl' | 'xxl'
};

const Loader = (props: Props): Element<*> => {
  return (
    <div styleName="loader" className={styles[`loader-${props.size}`]}>
      <Icon name="fc-ripple" size={props.size} />
    </div>
  );
};

Loader.defaultProps = {
  size: 'xl',
};

export default Loader;
