/* @flow */

// styles
import styles from './images.css';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import { ModalContainer } from '../modal/base';
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import SaveCancel from '../common/save-cancel';

// types
import type { TAlbum } from '../../modules/images';

type Props = {
  isVisible: boolean;
  isNew: boolean;
  loading: boolean;
  album: TAlbum;
  onSave: (name: string) => void;
  onCancel: () => void;
};

type State = {
  name: string;
}

class EditAlbum extends Component {
  static props: Props;

  state: State = {
    name: this.props.album.name,
  };

  _input: HTMLInputElement;

  componentDidMount(){
    this._input ? this._input.focus() : _.noop();
  }

  get closeAction(): Element {
    return <a onClick={this.props.onCancel}>&times;</a>;
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

  get saveDisabled(): boolean {
    return _.isEmpty(this.state.name) || this.state.name === this.props.album.name;
  }

  render(): Element {
    const title = this.props.isNew ? 'Add New Album' : 'Edit Album';

    return (
      <ModalContainer isVisible={this.props.isVisible}>
        <ContentBox title={title} actionBlock={this.closeAction}>
          <FormField label="Album Name"
                     className="fc-product-details__field"
                     labelClassName="fc-product-details__field-label"
          >
            <input type="text"
                   name="name"
                   className="fc-product-details__field-value"
                   value={this.state.name}
                   onChange={this.handleUpdateField}
                   ref={(i) => this._input = i}
            />
          </FormField>
          <SaveCancel onCancel={this.props.onCancel}
                      onSave={this.handleSave}
                      saveDisabled={this.saveDisabled}
                      isLoading={this.props.loading}
                      saveText="Save and Apply"
          />
        </ContentBox>
      </ModalContainer>
    );
  }
}

export default EditAlbum;
