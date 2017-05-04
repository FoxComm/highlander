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
import { FormField } from 'components/forms';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import wrapModal from 'components/modal/wrapper';
import Form from 'components/forms/form';
import TextInput from 'components/forms/text-input';
import ErrorAlerts from 'components/alerts/error-alerts';

// styles
import s from './edit-image.css';

// types
import type { NewAlbum } from '../../modules/images';

type Props = {
  onSave: (name: string) => void;
  onCancel: () => void;
  inProgress?: boolean;
  error?: any;
};

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
    return _.isEmpty(this.state.url);
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
    const { error, inProgress } = this.props;

    return (
      <ContentBox title="Upload From Link" actionBlock={this.closeAction}>
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
          <ErrorAlerts error={error} />
          <SaveCancel
            className={s.uploadByUrl}
            onCancel={this.props.onCancel}
            onSave={this.handleSave}
            saveDisabled={this.saveDisabled}
            isLoading={inProgress}
            cancelDisabled={inProgress}
            saveDisabled={inProgress}
            saveText="Upload"
          />
        </Form>
      </ContentBox>
    );
  }
}

const Wrapped: Class<Component<void, Props, State>> = wrapModal(UploadByUrl);

export default Wrapped;
