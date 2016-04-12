/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';

// helpers
import { getProductAttributes, setProductAttribute } from '../../paragons/product';

// types
import type {
  FullProduct,
} from '../../modules/products/details';

type Props = {
  product: FullProduct,
  onUpdateProduct: (product: FullProduct) => void,
  onSetProperty: (field: string, type: string, value: any) => void,
};

export default class ProductImages extends Component<void, Props, void> {
  static propTypes = {
    product: PropTypes.object.isRequired,
    onSetProperty: PropTypes.func.isRequired,
    onUpdateProduct: PropTypes.func.isRequired,
  };

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

  get images(): Array<?string> {
    const attributes = getProductAttributes(this.props.product);
    return _.get(attributes, 'images.value', [ null ]);
  }

  @autobind
  handleAddImage() {
    const newImages = [...this.images, null];
    this.updateImages(newImages);
  }

  @autobind
  handleRemoveImage(idx: number) {
    const images = [
      ...this.images.slice(0, idx),
      ...this.images.slice(idx + 1),
    ];

    this.props.onSetProperty('images', 'images', images);
  }

  @autobind
  handleUpdateImage(idx: number, event: Object) {
    const newImages = [
      ...this.images.slice(0, idx),
      event.target.value,
      ...this.images.slice(idx + 1),
    ];

    this.updateImages(newImages);
  }

  updateImages(images: Array<?string>) {
    const product = setProductAttribute(this.props.product, 'images', 'images', images);
    this.props.onUpdateProduct(product);
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
