/* @flow */

// libs
import React, { Component, Element } from 'react';

// styles
import styles from './gallery.css';


type State = {
  selected: number,
};

type Props = {
  images: Array<string>,
};

class Gallery extends Component {
  props: Props;

  state: State = {
    selected: 0,
  };

  setSelected(index: number): void {
    // handle bounds
    const selected = Math.min(Math.max(index, 0), this.props.images.length);

    this.setState({selected});
  }

  get hasPreviews(): boolean {
    return this.props.images.length > 1;
  }

  get previews(): Element<*> {
    const { selected } = this.state;

    return (
      <div styleName="previews">
        {this.props.images.map((image, index) => (
          <img
            key={`image-${index}`}
            src={image}
            styleName={index === selected ? 'selected' : null}
            onClick={() => this.setSelected(index)}
          />
        ))}
      </div>
    );
  }

  get currentImage(): Element<*> {
    return (
      <div styleName="image">
        <img src={this.props.images[this.state.selected]} />
      </div>
    );
  }

  render() {
    const styleName = this.hasPreviews ? 'multiple-gallery' : 'single-gallery';

    return (
      <div styleName={styleName}>
        {this.hasPreviews ? this.previews : null}
        {this.currentImage}
      </div>
    );
  }
}

export default Gallery;
