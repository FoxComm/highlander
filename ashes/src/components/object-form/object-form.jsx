/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import _ from 'lodash';

import ContentBox from '../content-box/content-box';
import ObjectFormInner from './object-form-inner';

type Attribute = { t: string, v: any };
type Attributes = { [key:string]: Attribute };

type Props = {
  canAddProperty?: boolean,
  className?: string,
  fieldsToRender?: Array<string>,
  attributes: Attributes,
  onChange: (attributes: Attributes) => void,
  title: string,
  options?: Object,
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
