
import React, { PropTypes } from 'react';
import Alert from './alert';

export default class ErrorAlerts extends React.Component {
  static propTypes = {
    errors: PropTypes.array
  };

  render() {
    if (this.props.errors) {
      return (
        <div className="fc-errors">
          {this.props.errors.map((error, index) => {
            return <Alert key={`error-${index}`} type="error">{error}</Alert>;
            })}
        </div>
      );
    }
    return null;
  }
}
