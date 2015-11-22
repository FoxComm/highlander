
import { get } from 'sprout-data';
import React, { PropTypes } from 'react';
import Alert from './alert';

function parseError(err) {
  if (!err) return null;

  return get(err, ['responseJson', 'errors'], [err.toString()]);
}

const ErrorAlerts = props => {
  const errors = props.errors || parseError(props.error);

  if (errors && errors.length) {
    return (
      <div className="fc-errors">
        {errors.map((error, index) => {
          return <Alert key={`error-${index}`} type={Alert.ERROR}>{error}</Alert>;
        })}
      </div>
    );
  }

  return <div></div>;
};

ErrorAlerts.propTypes = {
  errors: PropTypes.array,
  error: PropTypes.object
};

export default ErrorAlerts;
