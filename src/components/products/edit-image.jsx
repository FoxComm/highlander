/* @flow */

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import { FormField } from '../forms';
import wrapModal from '../modal/wrapper';
import ContentBox from '../content-box/content-box';
import SaveCancel from '../common/save-cancel';

// types
import type { ImageInfo } from '../../modules/images';

type Props = {
  title: string,
  alt: string,
  onSave: (info: ImageInfo) => void,
  onCancel: () => void,
};

class EditImage extends Component {
  static props: Props;

  state: ImageInfo = {
    title: this.props.title,
    alt: this.props.alt,
  };

  get closeAction(): Element {
    return <a onClick={this.props.onCancel}>&times;</a>;
  }

  @autobind
  handleUpdateField({ target }) {
    this.setState({ [target.name]: target.value });
  }

  @autobind
  handleSave(event) {
    event.preventDefault();
    this.props.onSave(this.state);
  }

  render(): Element {
    const saveDisabled = _.isEmpty(this.state.title);

    return (
      <div className="fc-product-details__custom-property">
        <div className="fc-modal-container">
          <ContentBox title="Edit Image" actionBlock={this.closeAction}>
            <FormField
              className="fc-product-details__field"
              label="Image Title"
              labelClassName="fc-product-details__field-label">
              <input
                type="text"
                className="fc-product-details__field-value"
                name="title"
                value={this.state.title}
                onChange={this.handleUpdateField} />
            </FormField>
            <FormField
              className="fc-product-details__field"
              label="Image Alt Text"
              labelClassName="fc-product-details__field-label">
              <input
                type="text"
                className="fc-product-details__field-value"
                name="alt"
                value={this.state.alt}
                onChange={this.handleUpdateField} />
            </FormField>
            <SaveCancel
              onCancel={this.props.onCancel}
              onSave={this.handleSave}
              saveDisabled={saveDisabled}
              saveText="Save and Apply" />
          </ContentBox>
        </div>
      </div>
    );
  }
}

export default wrapModal(EditImage);
