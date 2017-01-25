// @flow
// same as object-form-inner but designed for working with flat object structures without t,v fields
import _ from 'lodash';
import React from 'react';

import ObjectFormInner from './object-form-inner';
import type { Props as InnerProps } from './object-form-inner';

function objectFromAttributes(attributes: Attributes): Object {
  return _.reduce(attributes, (acc: Object, attr: Attribute|any, key: string) => {
    acc[key] = _.get(attr, 'v', attr);
    return acc;
  }, {});
}

type Props = InnerProps & {
  onChange: (attributes: Object) => void,
}

const ObjectFormFlat = (props: Props) => {
  const { onChange, ...rest } = props;

  const handleChange = (attributes: Attributes) => {
    onChange(objectFromAttributes(attributes));
  };

  return (
    <ObjectFormInner
      onChange={handleChange}
      {...rest}
    />
  );
};

export default ObjectFormFlat;


