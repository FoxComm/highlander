/* @flow */

// styles
import styles from './upload.css';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';
import { findDOMNode } from 'react-dom';

type Props = {
  children?: Element;
  onDrop?: Function;
}

type State = {
  dragActive: boolean;
}

export default class Upload extends Component {

  static props: Props;

  static defaultProps: Props = {
    onDrop: _.noop,
  };

  state:State = {
    dragActive: false,
  };

  @autobind
  onDragOver(e: SyntheticDragEvent): void {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';

    this.setState({
      dragActive: true,
    });
  }

  @autobind
  onDragLeave(): void {
    this.setState({
      dragActive: false,
    });
  }

  @autobind
  onDrop(e: any): void {
    e.preventDefault();

    this.setState({
      dragActive: false,
    });

    let files;

    if (e.dataTransfer) {
      files = e.dataTransfer.files;
    } else if (e.target) {
      files = e.target.files;
    }

    _.each(files, this.processFile);
  }

  @autobind
  processFile(file: File): void {
    const reader = new FileReader();

    reader.onloadend = () => this.props.onDrop({
      file: file,
      src: reader.result,
    });

    reader.readAsDataURL(file);
  }

  @autobind
  onClick(e: MouseEvent): void {
    console.log('onClick');

    this.refs.fileInput.click();
  }

  @autobind
  onItemClick(e: MouseEvent) {
    e.stopPropagation();
    e.preventDefault();

    console.log('onItemClick');
  }

  get container() {
    const { children } = this.props;

    let content;

    if (!React.Children.count(children)) {
      content = (
        <div className={styles.empty}>
          <i className="icon-upload" /> Drag & Drop to upload or click here
        </div>
      );
    } else {
      content = React.Children.map(
        this.props.children,
        child => React.cloneElement(child, { className: styles.uploadItem, onClick: this.onItemClick })
      );
    }

    const cls = classNames(styles.container, {
      [styles.dragActive]: this.state.dragActive,
    });

    return <div className={cls}
                onClick={this.onClick}
                onDragOver={this.onDragOver}
                onDragLeave={this.onDragLeave}
                onDrop={this.onDrop}>{content}</div>;
  }

  render(): Element {
    return (
      <div className={styles.upload}>
        <input className={styles.input} type="file" onChange={this.onDrop} ref="fileInput" />
        {this.container}
      </div>
    );
  }
}
