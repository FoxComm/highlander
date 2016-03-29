/**
 * @flow
 */

/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import { Form, FormField } from '../forms';
import { SliderCheckbox } from '../checkbox/checkbox';
import ContentBox from '../content-box/content-box';
import CurrencyInput from '../forms/currency-input';
import CustomProperty from './custom-property';
import DatePicker from '../datepicker/datepicker';
import ProductState from './product-state';
import RichTextEditor from '../rich-text-editor/rich-text-editor';
import SkuList from './sku-list';
import SubNav from './sub-nav';
import VariantList from './variant-list';
import WaitAnimation from '../common/wait-animation';

// helpers
import { getProductAttributes } from '../../paragons/product';

// types
import type {
  FullProduct,
} from '../../modules/products/details';

type Props = {
  product: FullProduct,
  onSetProperty: (field: string, type: string, value: any) => void,
};

type State = {
  images: Array<?String>,
};

export default class ProductImages extends Component<void, Props, State> {
  static propTypes = {
    product: PropTypes.object.isRequired,
    onSetProperty: PropTypes.func.isRequired,
  };

  state: State;

  constructor(props: Props) {
    super(props);

    const attributes = getProductAttributes(this.props.product);
    const { images } = attributes;
    const value = images.value.length > 0 ? images.value : [ null ];
    this.state = { images: value };
  }

  componentWillReceiveProps(nextProps: Props) {
    const attributes = getProductAttributes(nextProps.product);
    const { images } = attributes;
    const value = images.value.length > 0 ? images.value : [ null ];
    this.setState({ images: value });
  }


  get contentBox(): Element {
    const { images } = this.state;

    const imageControls = _.map(images, (val, idx) => {
      return (
        <div className="fc-product-details__image">
          <FormField
            className="fc-product-details__field"
            key={`product-image-page-field-${idx}`}>
            <input
              className="fc-product-details__field-value"
              type="text"
              value={val}
              onBlur={this.handleBlur}
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
  handleBlur() {
    this.props.onSetProperty('images', 'images', this.state.images);
  }

  @autobind
  handleAddImage() {
    this.setState({ images: [...this.state.images, null] });
  }

  @autobind
  handleRemoveImage(idx: number) {
    this.setState({
      images: [
        ...this.state.images.slice(0, idx),
        ...this.state.images.slice(idx + 1),
      ],
    })
  }

  @autobind
  handleUpdateImage(idx: number, event: Object) {
    const newImages = [
      ...this.state.images.slice(0, idx),
      event.target.value,
      ...this.state.images.slice(idx + 1),
    ];

    this.setState({ images: newImages });
  }

  render(): Element {
    return (
      <div className="fc-product-details fc-grid">
        <div className="fc-col-md-1-1">
          {this.contentBox}
        </div>
      </div>
    );
  }
}
