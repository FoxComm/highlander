/* @flow */

// libs
import { get } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import { DateTime } from 'components/common/datetime';
import { ModalContainer } from '../modal/base';
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import Input from 'components/forms/text-input';
import { DeleteButton } from 'components/core/button';
import ErrorAlerts from 'components/alerts/error-alerts';

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

  get closeAction() {
    return <a onClick={this.props.onCancel}>&times;</a>;
  }

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
    const { image: { src, createdAt }, inProgress, error } = this.props;
    const extMatch = src.match(/\.([0-9a-z]+)$/i);
    const nameMatch = src.match(/\/([^/]+)$/i);
    const ext = get(extMatch, '[1]', '–');
    const name = get(nameMatch, '[1]', '–');
    const style = { backgroundImage: `url('${src}')` };

    return (
      <ModalContainer isVisible={this.props.isVisible} size="big">
        <ContentBox title="Edit Image" actionBlock={this.closeAction} bodyClassName={s.body}>
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
                <Input value={src} disabled />
              </FormField>
              <FormField label="Alt Text" className={s.field} labelClassName={s.label}>
                <Input onChange={this.handleUpdateAltText} value={alt} />
              </FormField>
            </div>
          </div>
          <ErrorAlerts error={error} />
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
        </ContentBox>
      </ModalContainer>
    );
  }
}

export default EditImage;
