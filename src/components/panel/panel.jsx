import React, { PropTypes } from 'react';
import classNames from 'classnames';

const Panel = props => {
  return (
    <div className={classNames('fc-panel', props.className)}>
      <div className="fc-panel-header">
        {props.title}
      </div>
      <div className={classNames('fc-panel-content', {'fc-panel-content-featured': props.featured})}>
        {props.content && props.content.props.children}
        {props.children}
      </div>
    </div>
  );
};

Panel.propTypes = {
  children: PropTypes.any,
  title: PropTypes.string,
  content: PropTypes.any,
  featured: PropTypes.bool,
  className: PropTypes.string
};

Panel.defaultProps = {
  featured: false
};

export default Panel;
