/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// actions
import * as SkuActions from '../../modules/skus/details';

// components
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';

// helpers
import { illuminateAttributes, setAttribute } from '../../paragons/form-shadow-object';

// types
import type { FullSku } from '../../modules/skus/details';

type Props = {
  code: string,
  onChange: (sku: FullSku) => void,
  sku: ?FullSku,
};

export default class SkuImages extends Component<void, Props, void> {
  static propTypes = {
    code: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    sku: PropTypes.object,
  };

  get images(): Array<?string> {
    const formAttributes = _.get(this.props, 'sku.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'sku.shadow.attributes', []);
    const attributes = illuminateAttributes(formAttributes, shadowAttributes);
    return _.get(attributes, 'images.value', [ null ]);
  }

  get contentBox(): Element {
    const imageControls = _.map(this.images, (val, idx) => {
      return (
        <div className="fc-product-details__image">
          <FormField
            className="fc-product-details__field"
            key={`product-image-page-field-${idx}`}>
            <input
              className="fc-product-details__field-value"
              type="text"
              value={val}
              onChange={(e) => this.handleUpdateImage(idx, e)} />
          </FormField>
          <i className="icon-close" onClick={() => this.handleRemoveImage(idx)} />
        </div>
      );
    });

    return (
      <ContentBox title="Image URLs">
        {imageControls}
        <div className="fc-product-details__add-custom-property">
          New Image
          <a className="fc-product-details__add-custom-property-icon"
             onClick={this.handleAddImage}>
            <i className="icon-add" />
          </a>
        </div>
      </ContentBox>
    );
  }

  @autobind
  handleAddImage() {
    const images = [...this.images, null];
    this.handleChange(images);
  }

  @autobind
  handleRemoveImage(idx: number) {
    const images = [
      ...this.images.slice(0, idx),
      ...this.images.slice(idx + 1),
    ];

    this.handleChange(images);
  }

  @autobind
  handleUpdateImage(idx: number, event: Object) {
    const newImages = [
      ...this.images.slice(0, idx),
      event.target.value,
      ...this.images.slice(idx + 1),
    ];

    this.handleChange(newImages);
  }

  handleChange(images: Array<?string>) {
    const { sku } = this.props;
    const formAttributes = _.get(sku, 'form.attributes', []);
    const shadowAttributes = _.get(sku, 'shadow.attributes', []);
    const [ form, shadow ] = setAttribute('images', 'images', images, formAttributes, shadowAttributes);
    const newSku = assoc(sku,
      ['form', 'attributes'], form,
      ['shadow', 'attributes'], shadow
    );

    this.props.onChange(newSku);
  }

  render(): Element {
    return (
      <div className="fc-product-details fc-grid fc-grid-no-gutter">
        <div className="fc-col-md-1-1">
          {this.contentBox}
        </div>
      </div>
    );
  }
}
