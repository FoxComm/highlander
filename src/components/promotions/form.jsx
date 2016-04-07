
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import ObjectForm from '../object-form/object-form';

export default class PromotionForm extends Component {

  get generalAttrs() {
    const toOmit = [
    ];
    const shadow = _.get(this.props, 'promotion.shadow.attributes', []);
    return _(shadow).omit(toOmit).keys().value();
  }

  handleProductChange() {

  }

  render() {
    const formAttributes = _.get(this.props, 'promotion.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'promotion.shadow.attributes', []);

    return (
      <div>
        <ObjectForm
          onChange={this.handleProductChange}
          fieldsToRender={this.generalAttrs}
          form={formAttributes}
          shadow={shadowAttributes}
          title="General" />
      </div>
    );
  }
}
