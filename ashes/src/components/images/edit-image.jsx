/* @flow */

// libs
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import Modal from 'components/core/modal';
import { FormField } from '../forms';
import SaveCancel from 'components/core/save-cancel';
import TextInput from 'components/core/text-input';

// types
import type { ImageInfo } from '../../modules/images';

type Props = {
  isVisible: boolean,
  image: ImageInfo,
  onSave: (info: ImageInfo) => void,
  onCancel: () => void,
};

class EditImage extends Component {
  props: Props;

  state: ImageInfo = {
    src: this.props.image.src,
    title: this.props.image.title,
    alt: this.props.image.alt,
  };

  @autobind
  handleUpdateField(value: string, name: string) {
    this.setState({ [name]: value });
  }

  @autobind
  handleSave(event: Event) {
    event.preventDefault();
    this.props.onSave(this.state);
  }

  get footer() {
    const saveDisabled = !!this.state.title;

    return (
      <SaveCancel
        onCancel={this.props.onCancel}
        onSave={this.handleSave}
        saveDisabled={saveDisabled}
        saveLabel="Save and Apply"
      />
    );
  }

  render() {
    return (
      <Modal title="Edit Image" footer={this.footer} isVisible={this.props.isVisible} onClose={this.props.onCancel}>
        <FormField
          label="Image Title"
          className="fc-product-details__field"
          labelClassName="fc-product-details__field-label"
        >
          <TextInput name="title" value={this.state.title} onChange={this.handleUpdateField} />
        </FormField>
        <FormField
          label="Image Alt Text"
          className="fc-product-details__field"
          labelClassName="fc-product-details__field-label"
        >
          <TextInput name="alt" value={this.state.alt} onChange={this.handleUpdateField} />
        </FormField>
        <FormField
          label="Image URL"
          className="fc-product-details__field"
          labelClassName="fc-product-details__field-label"
        >
          <TextInput name="src" value={this.state.src} placeholder="http://" onChange={this.handleUpdateField} />
        </FormField>
      </Modal>
    );
  }
}

export default EditImage;
