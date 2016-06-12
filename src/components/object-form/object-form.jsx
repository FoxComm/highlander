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
  form: FormAttributes,
  shadow: ShadowAttributes,
  onChange: (form: FormAttributes, shadow: ShadowAttributes) => void,
  title: string,
  options?: Object,
};

export default class ObjectForm extends Component<void, Props, void> {
  props: Props;

  illuminateAttributes(form: FormAttributes, shadow: ShadowAttributes): Attributes {
    return _.reduce(shadow, (res, value, key) => {
      const formValue = _.get(form, [value.ref]);
      return {
        ...res,
        [key]: {
          t: value.type,
          v: formValue,
        },
      };
    }, {});
  }

  render(): Element {
    const { title, className, ...rest } = this.props;

    console.log('Illuminating the shadows');
    const { form, shadow } = this.props;
    console.log(this.illuminateAttributes(form, shadow));

    return (
      <ContentBox title={title} className={className}>
        <ObjectFormInner {...rest} />
      </ContentBox>
    );
  }
}
