
import React, { PropTypes } from 'react';
import styles from './site.css';

const Site = props => {
  return <div styleName="site">{props.children}</div>;
};

Site.propTypes = {
  children: PropTypes.node,
};

export default Site;
