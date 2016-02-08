
import _ from 'lodash';
import classNames from 'classnames';
import React, { PropTypes } from 'react';

const ContextBox = props => {
  let body = props.children;

  if (_.isEmpty(body)) {
    if (props.renderContent) body = props.renderContent();
    else if (props.viewContent) body = props.viewContent;
  }

  if (props.indentContent) {
    body = (
      <div className="fc-content-box-indent">
        {body}
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
      { props.footer }
    </div>
  );
};

ContextBox.propTypes = {
  title: PropTypes.node,
  className: PropTypes.string,
  actionBlock: PropTypes.node,
  children: PropTypes.node,
  footer: PropTypes.node,
  indentContent: PropTypes.bool,
  renderContent: PropTypes.func,
  viewContent: PropTypes.node,
};

ContextBox.defaultProps = {
  indentContent: true
};

export default ContextBox;
