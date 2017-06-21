/* @flow */

// libs
import { identity, isEmpty } from 'lodash';
import React from 'react';

// components
import Alert from 'components/core/alert';
import AutoScroll from 'components/utils/auto-scroll';

// styles
import s from './errors.css';

type CloseAction = () => any;

type Props = {
  /** Array of error strings */
  errors: Array<string>,
  /** Alert close callback */
  closeAction: CloseAction,
  /** Function to process error before rendering */
  sanitizeError: (error: string) => string,
  /** Additional className */
  className?: string,
};

const renderError = (closeAction: CloseAction) => (error: string, index: number) => {
  const handleClose = closeAction ? () => closeAction(error) : null;

  return (
    <Alert className={s.alert} type={Alert.ERROR} closeAction={handleClose} key={`${error}-${index}`}>
      {error}
    </Alert>
  );
};

/**
 * Component to render a list of alerts for errors array
 */
export default ({ errors, closeAction, sanitizeError = identity, className = '' }: Props) => {
  if (isEmpty(errors)) {
    return null;
  }

  const sanitizedErrors = errors.map(sanitizeError);
  const renderErrorFn = renderError(closeAction);

  return (
    <div className={className}>
      <AutoScroll />

      {sanitizedErrors.map(renderErrorFn)}
    </div>
  );
};
