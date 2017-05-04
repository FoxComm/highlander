/* @flow */

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

class EditAlbum extends Component {

  props: Props;

  state: State = {
    name: this.props.album.name,
  };

  static defaultProps = {
    isNew: false,
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
    const { error, inProgress } = this.props;
    const title = this.props.isNew ? 'Add New Album' : 'Edit Album';

    return (
      <ContentBox title={title} actionBlock={this.closeAction}>
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
              ref={r => this.input = r}
              autoComplete="off"
            />
          </FormField>
          <ErrorAlerts error={error} />
          <SaveCancel
            className={s.editAlbumFooter}
            onCancel={this.props.onCancel}
            onSave={this.handleSave}
            saveDisabled={this.saveDisabled || inProgress}
            cancelDisabled={inProgress}
            isLoading={inProgress}
            saveText="Save and Apply"
          />
        </Form>
      </ContentBox>
    );
  }
}

const Wrapped: Class<Component<void, Props, State>> = wrapModal(EditAlbum);

export default Wrapped;
