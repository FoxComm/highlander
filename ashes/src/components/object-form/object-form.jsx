/* @flow */

import React from 'react';

import ContentBox from '../content-box/content-box';
import ObjectFormInner from './object-form-inner';

type Props = {
  canAddProperty?: boolean,
  className?: string,
  fieldsToRender?: Array<string>,
  attributes: Attributes,
  onChange: (attributes: Attributes) => void,
  title: string,
  options?: Object,
};

const ObjectFrom = (props: Props) => {
  const { title, className, ...rest } = props;

  return (
    <ContentBox title={title} className={className}>
      <ObjectFormInner {...rest} />
    </ContentBox>
  );
};

export default ObjectFrom;
