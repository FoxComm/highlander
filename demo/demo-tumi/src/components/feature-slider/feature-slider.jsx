// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './feature-slider.css';

type Feature = {
  name: string,
  description: string,
  icon: string,
  imageUrl: string,
};

type Props = { features: Array<Feature> };
type State = { selected: string };

function convertUnicode(input) {
  return input.replace(/\\u(\w\w\w\w)/g,function(a,b) {
    var charcode = parseInt(b,16);
    return String.fromCharCode(charcode);
  });
}


export default class FeatureSlider extends Component {
  props: Props;
  state: State = { selected: '' };

  componentWillReceiveProps(nextProps: Props) {
    const names = _.keys(nextProps.features);
    if (names.length > 0) {
      this.setState({ selected: names[0] });
    }
  }

  renderIcons() {
    const { features } = this.props;
    const { selected } = this.state;
    const icons = _.map(features, (feature, name) => {
      const isSelected = name === selected;
      const styleName = isSelected ? 'icon-selected' : 'icon-unselected';
      const onClick = () => this.setState({ selected: name });
      if (feature.icon) {
        return (
            <button
            styleName={styleName}
            onClick={onClick}
            disabled={isSelected}
            key={feature.icon}
            >
            {convertUnicode(feature.icon)}
            </button>
        );
      }
    });

    return <div styleName="icons">{icons}</div>;
  };

  renderSelected() {
    const { features } = this.props;
    const { selected } = this.state;

    if (selected === '') return null;

    const { description, imageUrl } = features[selected];
    return (
      <div styleName="selected">
        <div styleName="summary">
          <div styleName="name">{this.state.selected}</div>
          <div
            styleName="description"
            dangerouslySetInnerHTML={{ __html: description }}
          />
        </div>
        <img src={imageUrl} />
      </div>
    );
  }

  render() {
    return (
      <div styleName="slider">
        {this.renderIcons()}
        {this.renderSelected()}
      </div>
    );
  }
}

