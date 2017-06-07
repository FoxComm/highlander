/* @flow */

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import Modal from 'components/core/modal';
import { FormField } from 'components/forms';
import SaveCancel from 'components/core/save-cancel';

// types
import type { NewAlbum } from '../../modules/images';

type Props = {
  isVisible: boolean;
  isNew?: boolean;
  loading: boolean;
  album: NewAlbum;
  onSave: (name: string) => void;
  onCancel: () => void;
  className?: string,
};

type State = {
  name: string;
};

export default class EditAlbum extends Component {

  props: Props;

  state: State = {
    name: this.props.album.name,
  };

  static defaultProps = {
    isNew: false,
  };

  _input: HTMLInputElement;

  componentDidMount() {
    this._input ? this._input.focus() : _.noop();
  }

  get saveDisabled(): boolean {
    return _.isEmpty(this.state.name) || this.state.name === this.props.album.name;
  }

  @autobind
  handleUpdateField({ target }: { target: HTMLInputElement }) {
    this.setState({ [target.name]: target.value });
  }

  @autobind
  handleSave(event: Event) {
    event.preventDefault();
    this.props.onSave(this.state.name);
  }

  get footer() {
    return (
      <SaveCancel
        onCancel={this.props.onCancel}
        onSave={this.handleSave}
        saveDisabled={this.saveDisabled}
        isLoading={this.props.loading}
        saveLabel="Save and Apply"
      />
    );
  }

  render() {
    const title = this.props.isNew ? 'Add New Album' : 'Edit Album';

    return (
      <Modal
        className={this.props.className}
        title={title}
        footer={this.footer}
        isVisible={this.props.isVisible}
        onClose={this.props.onCancel}
      >
        <FormField
          label="Album Name"
          className="fc-product-details__field"
          labelClassName="fc-product-details__field-label"
        >
          <input
            type="text"
            name="name"
            className="fc-product-details__field-value"
            value={this.state.name}
            onChange={this.handleUpdateField}
            ref={(i) => this._input = i}
          />
        </FormField>
      </Modal>
    );
  }
}
