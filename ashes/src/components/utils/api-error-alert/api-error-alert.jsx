/* @flow */

// libs
import { identity, isEmpty, get } from 'lodash';
import React from 'react';

// components
import Alert from 'compomemts/alert';
import AutoScroll from 'compomemts/utils/auto-scroll';

type Error = string | Object;

type CloseAction = () => any;

type Props = {
  /** Api response object */
  response: ?Object,
  /** Alert close callback */
  closeAction: CloseAction,
  /** Function to process error before rendering */
  sanitizeError: (error: Error) => any,
};

function parseError(err) {
  if (!err) return null;

  return get(err, 'response.body.errors', err.toString());
}

export default ({ error, closeAction, sanitizeError = identity }: Props) => {
  let err = parseError(error);

  if (isEmpty(err)) {
    return null;
  }

  err = sanitizeError(err);
  const handleClose = closeAction ? () => closeAction(error) : null;

  return (
    <div className={s.alert}>
      <AutoScroll />
      <Alert type={Alert.ERROR} closeAction={handleClose}>
        {err}
      </Alert>
    </div>
  );
};
