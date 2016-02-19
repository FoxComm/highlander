
import React, { PropTypes } from 'react';
import styles from './site.css';
import cssModules from 'react-css-modules';

const Site = props => {
  return <div styleName="site">{props.children}</div>;
};

Site.propTypes = {
  children: PropTypes.node,
};

export default cssModules(Site, styles);
