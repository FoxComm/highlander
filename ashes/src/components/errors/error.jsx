/* @flow */

import _ from 'lodash';
import React, { PropTypes } from 'react';

import styles from './error.css';

const NOT_FOUND_ERROR = 'There is no object that you are looking for.';
const UNEXPECTED_ERROR = 'Something went wrong. We are investigating it now.';

type ErrorObject = {
  status: ?number;
}

type Props = {
  notFound?: string;
  otherProblem?: string;
  err: ?ErrorObject;
};

const Error = ({
  err,
  notFound = NOT_FOUND_ERROR,
  otherProblem = UNEXPECTED_ERROR,
}: Props) => {
  const status = _.get(err, 'status');
  const message = status == 404 ? notFound : otherProblem;

  return (
    <div styleName="error">
      {message}
    </div>
  );
};

export default Error;
