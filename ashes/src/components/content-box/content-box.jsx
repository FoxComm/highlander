// libs
import _ from 'lodash';
import classNames from 'classnames';
import React from 'react';
import PropTypes from 'prop-types';

// styles
import s from './content-box.css';

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
    s.body,
    props.bodyClassName,
    {'fc-content-box-indent': props.indentContent}
  );

  return (
    <div id={props.id} className={ classNames(s.box, 'fc-content-box', props.className) }>
      <header className={s.header}>
        <div className={s.title}>{ props.title }</div>
        <div className={s.controls}>{ props.actionBlock }</div>
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
