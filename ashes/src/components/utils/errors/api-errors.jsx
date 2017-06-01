/* @flow */

// libs
import { isArray, get } from 'lodash';
import React from 'react';

// components
import Errors from './errors';

type Props = {
  /** Api response object */
  response: ?Object,
  /** Alert close callback */
  closeAction?: () => any,
  /** Function to process error before rendering */
  sanitizeError?: (error: string) => string,
  /** Additional className */
  className?: string,
};

function getErrorsFromResponse(response: ?Object): Array<string> {
  if (!response) return [];

  const errors = get(response, 'response.body.errors', response.toString());

  return isArray(errors) ? errors : [errors];
}

/**
 * Component to render alerts for API response errors build on top of `/lib/api` response structure
 *
 * @function
 */
export default ({ response, ...rest }: Props) => {
  const errors = getErrorsFromResponse(response);

  return (
    <Errors {...rest} errors={errors} />
  );
};
