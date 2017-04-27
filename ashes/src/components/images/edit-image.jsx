/* @flow */

// libs
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import { ModalContainer } from '../modal/base';
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import Input from 'components/forms/text-input-cm';
import { Button } from 'components/core/button';

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
  handleUpdateField({ target }: { target: HTMLInputElement }) {
    this.setState({ [target.name]: target.value });
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
    const { image: { src, createdAt }, inProgress } = this.props;
    const match = src.match(/\.[0-9a-z]+$/i);
    let ext = '–';

    if (match && match[0]) {
      ext = match[0];
    }

    return (
      <ModalContainer isVisible={this.props.isVisible} size="big">
        <ContentBox title="Edit Image" actionBlock={this.closeAction} bodyClassName={s.body}>
          <div className={s.main}>
            <div className={s.imageWrap}>
              <img src={src} className={s.image} />
            </div>
            <div className={s.sidebar}>
              <div className={s.stat}>
                <div className={s.statItem}>{`File Name: TODO`}</div>
                <div className={s.statItem}>{`Uploaded: ${createdAt || '–'}`}</div>
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
          <footer className={s.footer}>
            <Button onClick={this.props.onRemove} icon='trash' disabled={inProgress}>Delete</Button>
            <SaveCancel onSave={this.handleSave} onCancel={this.props.onCancel} isLoading={inProgress} />
          </footer>
        </ContentBox>
      </ModalContainer>
    );
  }
}

export default EditImage;
