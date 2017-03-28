
import classNames from 'classnames';
import React, { PropTypes } from 'react';

const ERROR = 'error';
const WARNING = 'warning';
const SUCCESS = 'success';

const Alert = props => {
  const className = classNames(
    `fc-alert is-alert-${props.type}`,
    props.className,
    {'is-can-be-closed': props.closeAction}
  );

  let closeButton = null;
  if (props.closeAction) {
    closeButton = <span onClick={props.closeAction} className="fc-btn-close" title="Close">&times;</span>;
  }

  return (
    <div className={className}>
      <i className={`icon-${props.type}`}></i>
      {props.children}
      {closeButton}
    </div>
  );
};

Alert.propTypes = {
  type: PropTypes.oneOf([
    ERROR, WARNING, SUCCESS
  ]).isRequired,
  closeAction: PropTypes.func,
  children: PropTypes.node,
  className: PropTypes.string,
};

Alert.ERROR = ERROR;
Alert.WARNING = WARNING;
Alert.SUCCESS = SUCCESS;

export default Alert;
