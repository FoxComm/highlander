/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

import ObjectScheduler from './object-scheduler';

type Attribute = { t: string, v: any };
type Attributes = { [key:string]: Attribute };

type Props = {
  form: FormAttributes,
  shadow: ShadowAttributes,
  onChange: (form: FormAttributes, shadow: ShadowAttributes) => void,
  title: string,
};

export default class FullObjectScheduler extends Component {
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
          ['shadow', key], { type: attr.v, ref: key });
      }
return res;
    }, { form, shadow });

    this.props.onChange(updatedAttributes.form, updatedAttributes.shadow);
  }

  render(): Element {
    const { form, shadow, onChange, ...rest } = this.props;
    const attributes = this.illuminateAttributes(form, shadow);
    const innerProps = { ...rest, attributes: attributes, onChange: this.handleChange };

    return (
      <ObjectScheduler {...innerProps} />
    );
  }
}
