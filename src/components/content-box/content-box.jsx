
import classNames from 'classnames';
import React, { PropTypes } from 'react';

const ContextBox = props => {
  let body = props.children;

  if (props.indentContent) {
    body = (
      <div className="fc-content-box-indent">
        {props.children}
      </div>
    );
  }

  return (
    <div className={ classNames('fc-content-box', props.className) }>
      <header className="fc-content-box-header">
        <div className="fc-grid">
          <div className="fc-col-md-2-3 fc-title">{ props.title }</div>
          <div className="fc-col-md-1-3 fc-controls">
            { props.actionBlock }
          </div>
        </div>
      </header>
      { body }
    </div>
  );
};

ContextBox.propTypes = {
  title: PropTypes.node,
  className: PropTypes.string,
  actionBlock: PropTypes.node,
  children: PropTypes.node,
  indentContent: PropTypes.bool
};

ContextBox.defaultProps = {
  indentContent: true
};

export default ContextBox;
