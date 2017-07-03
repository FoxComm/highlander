/* @flow */

// libs
import { get } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import { DateTime } from 'components/common/datetime';
import Modal from 'components/core/modal';
import { FormField } from '../forms';
import SaveCancel from 'components/core/save-cancel';
import TextInput from 'components/core/text-input';
import { DeleteButton } from 'components/core/button';
import Errors from 'components/utils/errors';

// types
import type { ImageInfo } from '../../modules/images';

// styles
import s from './edit-image.css';

type Props = {
  isVisible: boolean;
  image: ImageInfo;
  onSave: (info: ImageInfo) => void;
  onCancel: () => void;
  onRemove: Function;
  inProgress?: boolean;
  error?: string;
};

type State = {
  alt: string,
  width: number,
  height: number,
};

class EditImage extends Component {
  props: Props;

  state: State = {
    alt: this.props.image.alt || '',
    width: 0,
    height: 0,
  };

  img: ?Image;

  componentDidMount(): void {
    this.createImage();
  }

  componentWillUpdate(nextProps: Props) {
    if (this.props.image.src != nextProps.image.src) {
      this.createImage(nextProps.image.src);
    }
  }

  componentWillUnmount() {
    this.destroyImage();
  }

  createImage(src: string = this.props.image.src): void {
    // We need to create new image and load it again (from cache) to know its real size
    // Otherwise CSS may affect values
    this.img = new Image();
    this.img.onload = () => {
      if (this.img) {
        this.setState({ width: this.img.width, height: this.img.height });
      }
    };
    this.img.onerror = () => this.setState({ width: 0, height: 0 });
    this.img.src = src;
  }

  @autobind
  destroyImage(): void {
    this.img = null;
  }

  @autobind
  handleUpdateAltText(nextValue: string) {
    this.setState({ alt: nextValue });
  }

  @autobind
  handleSave(event: Event) {
    event.preventDefault();
    this.props.onSave({
      ...this.props.image,
      alt: this.state.alt,
    });
  }

  render() {
    const { width, height, alt } = this.state;
    const { image: { src, createdAt }, inProgress, error, onCancel } = this.props;
    const extMatch = src.match(/\.([0-9a-z]+)$/i);
    const nameMatch = src.match(/\/([^/]+)$/i);
    const ext = get(extMatch, '[1]', '–');
    const name = decodeURIComponent(get(nameMatch, '[1]', '–'));
    const decodedSrc = decodeURIComponent(src);
    const style = { backgroundImage: `url('${src}')` };

    return (
      <Modal title="Edit Image" isVisible={this.props.isVisible} onClose={onCancel} size="big">
        <div className={s.main}>
          <div className={s.image} style={style} />
          <div className={s.sidebar}>
            <div className={s.stat}>
              <div className={s.statItem}>{`File Name: ${name}`}</div>
              <div className={s.statItem}>Uploaded: <DateTime value={createdAt} /></div>
              <div className={s.statItem}>{`File Type: ${ext}`}</div>
              <div className={s.statItem}>{`Dimensions: ${width}×${height}`}</div>
            </div>
            <FormField label="URL" className={s.field} labelClassName={s.label}>
              <TextInput value={decodedSrc} disabled />
            </FormField>
            <FormField label="Alt Text" className={s.field} labelClassName={s.label}>
              <TextInput onChange={this.handleUpdateAltText} value={alt} />
            </FormField>
          </div>
        </div>
        <Errors error={error} />
        <footer className={s.footer}>
          <DeleteButton onClick={this.props.onRemove} disabled={inProgress}>Delete</DeleteButton>
          <SaveCancel
            onSave={this.handleSave}
            onCancel={this.props.onCancel}
            isLoading={inProgress}
            saveDisabled={inProgress}
            cancelDisabled={inProgress}
          />
        </footer>
      </Modal>
    );
  }
}

export default EditImage;
