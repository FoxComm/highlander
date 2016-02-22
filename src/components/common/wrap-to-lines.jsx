
import React, { PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './css/wrap-to-lines.css';

const WrapToLines = props => {
  return (
    <div styleName="block" {...props}>
      <div styleName="line"></div>
      <div styleName="content">{props.children}</div>
      <div styleName="line"></div>
    </div>
  );
};

WrapToLines.propTypes = {
  children: PropTypes.node,
};


export default cssModules(WrapToLines, styles);
