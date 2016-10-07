/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

import ObjectFormInner from './object-form-inner';

import type { Attribute, Attributes } from 'paragons/object';

type Props = {
  canAddProperty?: boolean,
  className?: string,
  fieldsToRender?: Array<string>,
  form: FormAttributes,
  shadow: ShadowAttributes,
  onChange: (form: FormAttributes, shadow: ShadowAttributes) => void,
  options?: Object,
};

export default class FullObjectForm extends Component {
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

  @autobind
  handleChange(attributes: Attributes) {
    const { form, shadow } = this.props;

    const updatedAttributes = _.reduce(attributes, (res, attr, key) => {
      const shadowAttr = res.shadow[key];
      const ref = _.get(shadowAttr, 'ref');
      const formAttr = res.form[ref];

      if (!shadowAttr || formAttr !== attr.v) {
        return assoc(res,
          ['form', key], attr.v,
          ['shadow', key], { type: attr.t, ref: key });
      }

      return res;
    }, { form, shadow });

    this.props.onChange(updatedAttributes.form, updatedAttributes.shadow);
  }

  render(): Element {
    const { form, shadow, onChange, ...rest } = this.props;
    const attributes = this.illuminateAttributes(form, shadow);

    return (
      <ObjectFormInner
        attributes={attributes}
        {...rest}
        onChange={this.handleChange}
      />
    );
  }
}
