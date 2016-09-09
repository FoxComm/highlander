/* @flow */

import classNames from 'classnames';
import React from 'react';

const ERROR = 'error';
const WARNING = 'warning';
const SUCCESS = 'success';


type Props = {
  type: string;
  closeAction?: Function;
  children: NodeType;
  className?: string;
}

const Alert = (props: Props) => {
  const className = classNames(
    'fc-alert',
    props.className,
    `_type_${props.type}`,
    {'_closable': props.closeAction},
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

Alert.ERROR = ERROR;
Alert.WARNING = WARNING;
Alert.SUCCESS = SUCCESS;

export default Alert;
