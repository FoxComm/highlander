
import { get } from 'sprout-data';
import React, { PropTypes } from 'react';
import Alert from './alert';
import AutoScroll from '../common/auto-scroll';

function parseError(err) {
  if (!err) return null;

  return get(err, ['responseJson', 'errors'], [err.toString()]);
}

const ErrorAlerts = props => {
  const errors = props.errors || parseError(props.error);
  const closeAction = props.closeAction ? () => props.closeAction(error, index) : null;

  if (errors && errors.length) {
    return (
      <div className="fc-errors">
        <AutoScroll />
        {errors.map((error, index) => {
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

  return <div></div>;
};

ErrorAlerts.propTypes = {
  errors: PropTypes.array,
  error: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
  closeAction: PropTypes.func
};

export default ErrorAlerts;
