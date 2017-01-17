import _ from 'lodash';
import classNames from 'classnames';
import React, { PropTypes } from 'react';

const ContextBox = props => {
  let body = props.children;

  if (_.isEmpty(body)) {
    if (props.renderContent) {
      body = props.renderContent();
    } else if (props.viewContent) {
      body = props.viewContent;
    }
  }

  const bodyClassName = classNames(
    props.bodyClassName,
    {'fc-content-box-indent': props.indentContent}
  );

  return (
    <div id={props.id} className={ classNames('fc-content-box', props.className) }>
      <header className="fc-content-box-header">
        <div className="fc-title">{ props.title }</div>
        <div className="fc-controls">{ props.actionBlock }</div>
      </header>
      <div className={ bodyClassName }>
        { body }
      </div>
      { props.footer }
    </div>
  );
};

ContextBox.propTypes = {
  id: PropTypes.string,
  title: PropTypes.node,
  className: PropTypes.string,
  bodyClassName: PropTypes.string,
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
