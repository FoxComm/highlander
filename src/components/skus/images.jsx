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
import { illuminateAttributes } from '../../paragons/form-shadow-object';

// types
import type { FullSku, SkuState } from '../../modules/skus/details';

type Props = {
  actions: {
    fetchSku: (code: string, context?: string) => void,
  },
  code: string,
  skus: SkuState,
};

type State = {
  images: Array<?string>,
};

function setImages(props: Props): State {
  const formAttributes = _.get(props, 'skus.sku.form.attributes', []);
  const shadowAttributes = _.get(props, 'skus.sku.shadow.attributes', []);
  const attributes = illuminateAttributes(formAttributes, shadowAttributes);
  const imageValue = _.get(attributes, 'images.value', [ null ]);
  return { images: imageValue };
}
 
export default class SkuImages extends Component<void, Props, State> {
  static propTypes = {
    actions: PropTypes.shape({
      fetchSku: PropTypes.func.isRequired,
    }).isRequired,
    code: PropTypes.string.isRequired,
    skus: PropTypes.object,
  };

  state: State;

  constructor(props: Props, ...args: any) {
    super(props, ...args);
    this.state = setImages(this.props);
  }

  componentDidMount() {
    this.props.actions.fetchSku(this.props.code);
  }

  componentWillReceiveProps(nextProps: Props) {
    this.state = setImages(nextProps);
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
    this.setState({ images: [...this.state.images, null] });
  }

  @autobind
  handleRemoveImage(idx: number) {
    const images = [
      ...this.state.images.slice(0, idx),
      ...this.state.images.slice(idx + 1),
    ];

    //this.props.onSetProperty('images', 'images', images);
  }

  @autobind
  handleUpdateImage(idx: number, event: Object) {
    const newImages = [
      ...this.state.images.slice(0, idx),
      event.target.value,
      ...this.state.images.slice(idx + 1),
    ];

    //this.props.onSetProperty('images', 'images', newImages);
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
