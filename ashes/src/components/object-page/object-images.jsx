/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';

// components
import Images from '../images/images';

export function connectImages(namespace, actions) {
  const plural = `${namespace}s`;

  const mapStateToProps = (state) => ({
    namespace,
    albums: _.get(state, [plural, 'images', 'albums'], []),
    isLoading: _.get(state, ['asyncActions', `${plural}FetchAlbums`, 'inProgress'], true),
    addAlbumInProgress: _.get(state, ['asyncActions', `${plural}AddAlbum`, 'inProgress'], false),
    editAlbumInProgress: _.get(state, ['asyncActions', `${plural}EditAlbum`, 'inProgress'], false),
    uploadImagesInProgress: _.get(state, ['asyncActions', `${plural}UploadImages`, 'inProgress'], false),
  });

  return ImagesPage => {
    return connect(mapStateToProps, actions)(ImagesPage);
  };
}


export default class ImagesPage extends Component {

  get entityIdName(): string {
    return `${this.props.namespace}Id`;
  }

  get entityId(): string {
    return this.props.params[this.entityIdName];
  }

  get contextName(): string {
    return this.props.params.context;
  }

  componentDidMount(): void {
    this.props.fetchAlbums(this.contextName, this.entityId);
  }

  render(): Element {
    const { params, ...rest } = this.props;

    return (
      <Images {...rest} entityId={this.entityId} context={this.contextName} />
    );
  }
}

