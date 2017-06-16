/* @flow */

/**
 * Upload From Link   x
 * Media URL
 * <input>
 * cancel | upload
 */

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import Modal from 'components/core/modal';
import { FormField } from 'components/forms';
import SaveCancel from 'components/core/save-cancel';
import Form from 'components/forms/form';
import TextInput from 'components/core/text-input';
import Errors from 'components/utils/errors';

// styles
import s from './edit-image.css';

type Props = {|
  onSave: (name: string) => void;
  onCancel: () => void;
  inProgress?: boolean;
  error?: any;
  isVisible: boolean;
|};

type State = {
  url: string;
};

class UploadByUrl extends Component {

  props: Props;

  state: State = {
    url: '',
  };

  input: TextInput;

  componentDidMount() {
    if (this.input) {
      this.input.focus();
    }
  }

  get closeAction() {
    return <a onClick={this.props.onCancel}>&times;</a>;
  }

  get saveDisabled(): boolean {
    return _.isEmpty(this.state.url) || !!this.props.inProgress;
  }

  @autobind
  handleUpdateName(url: string) {
    this.setState({ url });
  }

  @autobind
  handleSave(event: Event) {
    event.preventDefault();
    this.props.onSave(this.state.url);
  }

  render() {
    const { error, inProgress, onCancel, isVisible } = this.props;

    return (
      <Modal title="Upload From Link" onClose={onCancel} isVisible={isVisible}>
        <Errors error={error} />
        <Form onSubmit={this.handleSave}>
          <FormField
            label="Media URL"
            className="fc-product-details__field"
            labelClassName="fc-product-details__field-label">
            <TextInput
              name="url"
              className="fc-product-details__field-value"
              value={this.state.url}
              onChange={this.handleUpdateName}
              ref={r => this.input = r}
              autoComplete="off"
            />
          </FormField>
          <SaveCancel
            className={s.uploadByUrl}
            onCancel={onCancel}
            onSave={this.handleSave}
            isLoading={inProgress}
            cancelDisabled={inProgress}
            saveDisabled={this.saveDisabled}
            saveText="Upload"
          />
        </Form>
      </Modal>
    );
  }
}

export default UploadByUrl;
