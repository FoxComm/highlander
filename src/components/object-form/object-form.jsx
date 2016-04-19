/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';

import ContentBox from '../content-box/content-box';
import ObjectFormInner from './object-form-inner';

type Props = {
  canAddProperty?: boolean,
  className?: string,
  fieldsToRender?: Array<string>,
  form: FormAttributes,
  shadow: ShadowAttributes,
  onChange: (form: FormAttributes, shadow: ShadowAttributes) => void,
  title: string,
};

export default class ObjectForm extends Component<void, Props, void> {
  props: Props;

  render(): Element {
    const { title, className, ...rest } = this.props;

    return (
      <ContentBox title={title} className={className}>
        <ObjectFormInner {...rest} />
      </ContentBox>
    );
  }
}
