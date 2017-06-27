/* @flow */

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import { FormField } from 'components/forms';
import Modal from 'components/core/modal';
import SaveCancel from 'components/core/save-cancel';
import Form from 'components/forms/form';
import TextInput from 'components/core/text-input';
import Errors from 'components/utils/errors';

// types
import type { NewAlbum } from '../../modules/images';

// styles
import s from './edit-image.css';

type Props = {
  isVisible: boolean;
  isNew?: boolean;
  inProgress: boolean;
  album: NewAlbum;
  onSave: (name: string) => void;
  onCancel: () => void;
  error?: any;
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

  _input: TextInput;

  componentDidMount() {
    if (this._input) {
      this._input.focus();
    }
  }

  get closeAction() {
    return <a onClick={this.props.onCancel}>&times;</a>;
  }

  get saveDisabled(): boolean {
    return _.isEmpty(this.state.name) || this.state.name === this.props.album.name;
  }

  @autobind
  handleUpdateName(name: string) {
    this.setState({ name });
  }

  @autobind
  handleSave(event: Event) {
    const { inProgress, onSave } = this.props;

    event.preventDefault();

    if (!inProgress && !this.saveDisabled) {
      onSave(this.state.name);
    }
  }

  render() {
    const { error, inProgress, onCancel, isVisible } = this.props;
    const title = this.props.isNew ? 'Add New Album' : 'Edit Album';

    return (
      <Modal title={title} onClose={onCancel} isVisible={isVisible}>
        <Form onSubmit={this.handleSave}>
          <FormField
            label="Album Name"
            className="fc-product-details__field"
            labelClassName="fc-product-details__field-label">
            <TextInput
              name="name"
              className="fc-product-details__field-value"
              value={this.state.name}
              onChange={this.handleUpdateName}
              ref={r => this._input = r}
              autoComplete="off"
            />
          </FormField>
          <Errors error={error} />
          <SaveCancel
            className={s.editAlbumFooter}
            onCancel={onCancel}
            onSave={this.handleSave}
            saveDisabled={this.saveDisabled || inProgress}
            cancelDisabled={inProgress}
            isLoading={inProgress}
            saveText="Save and Apply"
          />
        </Form>
      </Modal>
    );
  }
}
