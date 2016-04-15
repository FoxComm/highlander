/* @flow */

import _ from 'lodash';
import React from 'react';

import styles from './error.css';

type ErrorObject = {
  status: ?number;
}

type Props = {
  notFound: string;
  otherProblem: string;
  err: ErrorObject;
};

const Error = (props: Props) => {
  const status = _.get(props, 'err.status');
  const message = status == 404 ? props.notFound : props.otherProblem;

  return (
    <div styleName="error">
      {message}
    </div>
  );
};

Error.defaultProps = {
  notFound: 'There is no object that you are looking for.',
  otherProblem: 'Something went wrong. We are investigating it now.',
};

export default Error;
