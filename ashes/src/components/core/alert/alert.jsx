/** @flow */

// libs
import classNames from 'classnames';
import React from 'react';

// components
import Icon from 'components/core/icon';

// styles
import s from './alert.css';

const ERROR = 'error';
const WARNING = 'warning';
const SUCCESS = 'success';

type Props = {
  /** Alert type. error|warning|success */
  type: 'error' | 'warning' | 'success', // https://github.com/facebook/flow/issues/2377
  /** Alert close callback */
  closeAction: () => any,
  /** Alert content */
  children: Element<any>,
  /** Additional className */
  className?: string,
};

const Alert = (props: Props) => {
  const className = classNames(s.alert, s[props.type], {
    [s.closable]: props.closeAction,
  }, props.className);

  let closeButton = null;
  if (props.closeAction) {
    closeButton = <span className={s.close} onClick={props.closeAction} title="Close">&times;</span>;
  }

  return (
    <div className={className}>
      <div className={s.message}>
        <Icon name={props.type} />
        {props.children}
      </div>
      {closeButton}
    </div>
  );
};

Alert.ERROR = ERROR;
Alert.WARNING = WARNING;
Alert.SUCCESS = SUCCESS;

export default Alert;
