
import React, { PropTypes } from 'react';
import cssModules from 'react-css-modules';

import styles from './css/link.css';

const Link = props => {
  return <a styleName="link" {...props}>{props.children}</a>;
};

Link.propTypes = {
  children: PropTypes.node,
};

export default cssModules(Link, styles);
