/* @flow */

// libs
import { get } from 'lodash';
import React from 'react';

// components
import Errors from './errors';

type Props = {
  /** Api response object */
  response: ?Object,
  /** Alert close callback */
  closeAction: () => any,
  /** Function to process error before rendering */
  sanitizeError: (error: string) => string,
};

function getErrorsFromResponse(response: ?Object): Array<string> {
  if (!response) return [];

  return get(response, 'response.body.errors', [response.toString()]);
}

/**
 * Component to render alerts for API response errors build on top of /lib/api response structure
 */
export default ({ response, ...rest }: Props) => {
  const errors = getErrorsFromResponse(response);

  return (
    <Errors errors={errors} {...rest} />
  );
};
