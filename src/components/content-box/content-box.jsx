
import classNames from 'classnames';
import React, { PropTypes } from 'react';

const ContextBox = props => {
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
      <article className={ classNames('fc-content-box-content', {'fc-content-box-table' : props.isTable}) }>
        { props.children }
      </article>
      {props.footer}
    </div>
  );
};

ContextBox.propTypes = {
  title: PropTypes.node,
  className: PropTypes.string,
  actionBlock: PropTypes.node,
  children: PropTypes.node,
  isTable: PropTypes.bool,
  footer: PropTypes.node
};

export default ContextBox;
