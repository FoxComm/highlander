/* @flow */

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import { ModalContainer } from '../modal/base';
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import SaveCancel from '../common/save-cancel';

// types
import type { ImageInfo } from '../../modules/images';

type Props = {
  isVisible: boolean;
  image: ImageInfo;
  onSave: (info: ImageInfo) => void;
  onCancel: () => void;
};

class EditImage extends Component {
  props: Props;

  state: ImageInfo = {
    src: this.props.image.src,
    title: this.props.image.title,
    alt: this.props.image.alt,
  };

  get closeAction() {
    return <a onClick={this.props.onCancel}>&times;</a>;
  }

  @autobind
  handleUpdateField({ target }: { target: HTMLInputElement }) {
    this.setState({ [target.name]: target.value });
  }

  @autobind
  handleSave(event: Event) {
    event.preventDefault();
    this.props.onSave(this.state);
  }

  render() {
    const saveDisabled = _.isEmpty(this.state.title);

    return (
      <ModalContainer isVisible={this.props.isVisible}>
        <ContentBox title="Edit Image" actionBlock={this.closeAction}>
          <FormField label="Image Title"
                     className="fc-product-details__field"
                     labelClassName="fc-product-details__field-label"
          >
            <input type="text"
                   className="fc-product-details__field-value"
                   name="title"
                   value={this.state.title}
                   onChange={this.handleUpdateField}
            />
          </FormField>
          <FormField label="Image Alt Text"
                     className="fc-product-details__field"
                     labelClassName="fc-product-details__field-label">
            <input type="text"
                   className="fc-product-details__field-value"
                   name="alt"
                   value={this.state.alt}
                   onChange={this.handleUpdateField}
            />
          </FormField>
          <FormField label="Image URL"
                     className="fc-product-details__field"
                     labelClassName="fc-product-details__field-label">
            <input type="text"
                   className="fc-product-details__field-value"
                   name="src"
                   value={this.state.src}
                   placeholder="http://"
                   onChange={this.handleUpdateField}
             />
          </FormField>
          <SaveCancel onCancel={this.props.onCancel}
                      onSave={this.handleSave}
                      saveDisabled={saveDisabled}
                      saveText="Save and Apply"
          />
        </ContentBox>
      </ModalContainer>
    );
  }
}

export default EditImage;
