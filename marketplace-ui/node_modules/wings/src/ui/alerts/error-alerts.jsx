/* @flow */

import _ from 'lodash';
import React from 'react';
import Alert from './alert';
import AutoScroll from '../common/auto-scroll';

function parseError(err) {
  if (!err) return null;

  return _.get(err, ['responseJson', 'errors'], [err.toString()]);
}

type Props = {
  errors?: Array<string>;
  error?: Object|string;
  closeAction?: Function;
}

const ErrorAlerts = (props: Props) => {
  const errors = props.errors || parseError(props.error);

  if (errors && errors.length) {
    return (
      <div className="fc-errors">
        <AutoScroll />
        {errors.map((error, index) => {
          // $FlowFixMe: ternary operator flow, do you hear it ?
          const closeAction = props.closeAction ? () => props.closeAction(error, index) : null;

          return (
            <Alert
              key={`error-${error}-${index}`}
              type={Alert.ERROR}
              closeAction={closeAction}>
              {error}
            </Alert>
          );
        })}
      </div>
    );
  }

  return null;
};


export default ErrorAlerts;
