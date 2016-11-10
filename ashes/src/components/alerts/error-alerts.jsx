
import _ from 'lodash';
import React, { PropTypes } from 'react';
import Alert from './alert';
import AutoScroll from '../common/auto-scroll';

function parseError(err) {
  if (!err) return null;

  return _.get(err, 'response.body.errors', [err.toString()]);
}

const ErrorAlerts = props => {
  let errors = props.errors || parseError(props.error);

  if (props.sanitizeError) {
    errors = _.map(errors, props.sanitizeError);
  }

  if (errors && errors.length) {
    return (
      <div className="fc-errors">
        <AutoScroll />
        {errors.map((error, index) => {
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

ErrorAlerts.propTypes = {
  errors: PropTypes.array,
  error: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
  closeAction: PropTypes.func,
  sanitizeError: PropTypes.func,
};

export default ErrorAlerts;
