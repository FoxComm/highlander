/* @flow */

// libs
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// components
import { ModalContainer } from '../modal/base';
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import Input from 'components/forms/text-input';
import { Button } from 'components/common/buttons';

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
};

class EditImage extends Component {
  props: Props;

  state: ImageInfo = {
    src: this.props.image.src,
    title: this.props.image.title,
    alt: this.props.image.alt,
  };

  image: any;

  componentDidUpdate() {
    // console.log('this.image', this.image);
    if (this.image) {
    }
  }

  get closeAction() {
    return <a onClick={this.props.onCancel}>&times;</a>;
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
    this.props.onSave(this.state);
  }

  render() {
    const { src } = this.state;
    const saveDisabled = !!this.state.title;
    const match = src.match(/\.[0-9a-z]+$/i);
    let ext = '–';
    let size = '–';

    if (match && match[0]) {
      ext = match[0];
    }

    if (this.image) {
      size = `${this.image.width}×${this.image.height}`;
    }

    return (
      <ModalContainer isVisible={this.props.isVisible} size="big">
        <ContentBox title="Edit Image" actionBlock={this.closeAction} bodyClassName={s.body}>
          <div className={s.main}>
            <div className={s.imageWrap}>
              <img ref={ref => {console.log('ref', ref);this.image = ref}} src={src} className={s.image} />
            </div>
            <div className={s.sidebar}>
              <div className={s.stat}>
                <div className={s.statItem}>{`File Name: ${this.state.title}`}</div>
                <div className={s.statItem}>{`Uploaded: ${this.props.image.createdAt || '–'}`}</div>
                <div className={s.statItem}>{`File Type: ${ext}`}</div>
                <div className={s.statItem}>{`Dimensions: ${size}`}</div>
              </div>
              <FormField label="URL" className={s.field} labelClassName={s.label}>
                <Input value={this.state.src} disabled />
              </FormField>
              <FormField label="Alt Text" className={s.field} labelClassName={s.label}>
                <Input onChange={this.handleUpdateAltText} value={this.state.alt || ''} />
              </FormField>
            </div>
          </div>
          <footer className={s.footer}>
            <Button onClick={this.props.onRemove} icon='trash'>Delete</Button>
            <SaveCancel onSave={this.handleSave} onCancel={this.props.onCancel} saveDisabled={saveDisabled} />
          </footer>
        </ContentBox>
      </ModalContainer>
    );
  }
}

export default EditImage;
